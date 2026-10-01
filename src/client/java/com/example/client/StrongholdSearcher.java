package com.example.client;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EyeOfEnder;

/**
 * 末影之眼定位末地要塞的核心逻辑。
 *
 * <p>整个模组只在客户端运行，并且只有玩家在聊天栏输入 {@code @start} 之后才会工作；
 * 其余时间是休眠状态，不采集任何坐标、也不影响玩家的任何游戏行为。</p>
 *
 * <p>原理：末影之眼被扔出后会朝最近的要塞水平漂移，其平面轨迹是一条线段。
 * 记下同一只末影之眼在两个不同时刻的 {@code (x, z)} 坐标即可得到它所在直线
 * {@code z = kx + b}；玩家换一个位置再扔一次，两条直线的交点就是要塞坐标。</p>
 */
public final class StrongholdSearcher {
	public static final StrongholdSearcher INSTANCE = new StrongholdSearcher();

	/** 判断"周围有其他正在运动的末影之眼"的半径。 */
	private static final double INTERFERENCE_RADIUS = 16.0D;
	/** 两次扔出末影之眼时玩家所在位置必须达到的距离。 */
	private static final double MIN_THROW_DISTANCE = 32.0D;
	/** 末影之眼必须离玩家足够近，才认为这是玩家刚扔出的那一颗。 */
	private static final double DETECT_RADIUS = 32.0D;
	/** 交点离玩家超过这个距离就认为计算失败了。 */
	private static final double MAX_STRONGHOLD_DISTANCE = 20000.0D;
	/** 认为末影之眼"正在运动"的最小速度平方。 */
	private static final double MOVING_EPSILON = 0.02D;

	private enum State {
		/** 休眠。 */
		OFF,
		/** 等待第一颗末影之眼。 */
		WAIT_FIRST,
		/** 等待第二颗末影之眼。 */
		WAIT_SECOND,
		/** 需要玩家换到更远的地方再扔（32 格限制）。 */
		WAIT_FAR,
		/** 周围有其他正在运动的末影之眼，等它们消失后再扔。 */
		WAIT_CLEAR
	}

	private State state = State.OFF;

	/** 当前是否处于"已经提示玩家扔末影之眼、正在等他扔出来"的阶段。 */
	private boolean awaitingThrow;

	/** 正在跟踪的末影之眼（按对象身份区分，同一刻周围的多颗不会互相干扰）。 */
	private final Map<EyeOfEnder, EyeTracker> trackers = new IdentityHashMap<>();

	private Trajectory firstLine;
	private double firstPlayerX;
	private double firstPlayerZ;

	/** 已经采集完成的那一颗末影之眼，避免被重复当作新扔出的一颗。 */
	private EyeOfEnder lastCompletedEye;

	/** 上一次 tick 时的世界，用于检测玩家换世界。 */
	private ClientLevel trackedLevel;

	private StrongholdSearcher() {
	}

	// ------------------------------------------------------------------
	// 玩家指令
	// ------------------------------------------------------------------

	/** 玩家输入 {@code @start}：进入计算状态。 */
	public void start() {
		if (this.state != State.OFF) {
			// 已经在计算状态中，@start 不做任何处理，也不提示。
			return;
		}

		this.clearRound();
		this.state = State.WAIT_FIRST;
		this.awaitingThrow = true;
		print(Messages.throwFirstEye());
	}

	/** 玩家输入 {@code @stop}：强行结束计算状态并清空已经获取的坐标和计算结果。 */
	public void stop() {
		if (this.state == State.OFF) {
			// 休眠状态下 @stop 不做任何处理，也不提示。
			return;
		}

		this.reset();
		print(Messages.endCalculation());
	}

	/**
	 * 玩家输入 {@code @language <English|Chinese>}：切换模组的提示语言。
	 *
	 * <p>语言名忽略首字母大小写，切换成功后在聊天栏给出对应语言的提示。</p>
	 *
	 * @return 是否切换成功；语言名无法识别时返回 false（调用方当作普通聊天处理）
	 */
	public boolean switchLanguage(String name) {
		Language language = Language.parse(name);

		if (language == null) {
			return false;
		}

		Language.setCurrent(language);
		print(Messages.languageSwitched());

		return true;
	}

	// ------------------------------------------------------------------
	// 每客户端刻
	// ------------------------------------------------------------------

	/**
	 * 玩家退出世界/退出游戏时调用（由 {@code ClientLevelMixin} 在
	 * {@code ClientLevel.disconnect} 时触发）：立即退出计算状态，
	 * 丢弃所有已经记录的坐标和计算结果。
	 */
	public void finish() {
		this.reset();
		this.trackedLevel = null;
	}

	public void tick(Minecraft client) {
		if (this.state == State.OFF) {
			// 休眠状态：不采集坐标，也不参与玩家的任何游戏行为。
			return;
		}

		ClientLevel level = client.level;
		LocalPlayer player = client.player;

		if (level == null || player == null) {
			// 玩家退出世界/退出游戏：直接退出计算状态，不保存任何结果与坐标。
			this.reset();
			return;
		}

		if (level != this.trackedLevel) {
			// 换了世界，之前记录的坐标全部失效。
			this.trackedLevel = level;
			this.clearRound();
		}

		this.trackEyes(level);

		if (this.state == State.WAIT_CLEAR) {
			// 干扰的末影之眼消失之前不再接收新的扔出动作。
			if (!this.hasAnyOtherMovingEye(level, player)) {
				this.state = State.WAIT_FIRST;
				this.awaitingThrow = true;
				print(Messages.throwFirstEye());
			}

			return;
		}

		if (this.state == State.WAIT_FAR) {
			// 玩家必须离开上一次扔出地点至少 32 格，才接受第二次扔出。
			double dx = player.getX() - this.firstPlayerX;
			double dz = player.getZ() - this.firstPlayerZ;

			if (Math.sqrt(dx * dx + dz * dz) >= MIN_THROW_DISTANCE) {
				this.state = State.WAIT_SECOND;
				this.awaitingThrow = true;
				print(Messages.throwSecondEye());
			}

			return;
		}

		if (this.awaitingThrow) {
			this.scan(level, player);
		}
	}

	// ------------------------------------------------------------------
	// 末影之眼的检测与记录
	// ------------------------------------------------------------------

	/** 推进所有正在跟踪的末影之眼。 */
	private void trackEyes(ClientLevel level) {
		if (this.trackers.isEmpty()) {
			return;
		}

		List<EyeOfEnder> done = new ArrayList<>();

		for (Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof EyeOfEnder eye)) {
				continue;
			}

			EyeTracker tracker = this.trackers.get(eye);

			if (tracker != null && tracker.sample(eye)) {
				done.add(eye);
			}
		}

		for (EyeOfEnder eye : done) {
			EyeTracker tracker = this.trackers.remove(eye);

			if (tracker != null && tracker.hasEnoughSamples()) {
				this.onSamplesReady(eye, tracker);
			}
		}
	}

	/** 搜索玩家附近"刚被扔出"的末影之眼并开始记录。 */
	private void scan(ClientLevel level, LocalPlayer player) {
		List<EyeOfEnder> candidates = new ArrayList<>();

		for (Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof EyeOfEnder eye) || eye.isRemoved() || eye == this.lastCompletedEye) {
				continue;
			}

			if (this.trackers.containsKey(eye)) {
				continue;
			}

			if (eye.distanceToSqr(player.getX(), player.getY(), player.getZ()) <= DETECT_RADIUS * DETECT_RADIUS) {
				candidates.add(eye);
			}
		}

		if (candidates.isEmpty()) {
			return;
		}

		// 玩家半径 16 格内有正在运动的末影之眼时会干扰结果，这一颗不计入。
		// （刚刚结算完的那一颗本身可能还在飞，但它已经采集过了，不算干扰。）
		if (this.hasMovingEyeNearby(level, player, candidates.get(0), this.lastCompletedEye)) {
			print(Messages.otherEyesNearby());
			this.awaitingThrow = false;
			this.state = State.WAIT_CLEAR;
			return;
		}

		this.awaitingThrow = false;
		this.trackers.put(candidates.get(0), new EyeTracker(player));
	}

	private void onSamplesReady(EyeOfEnder eye, EyeTracker tracker) {
		this.lastCompletedEye = eye;

		if (this.state == State.WAIT_FIRST) {
			this.handleFirstEye(tracker);
		} else if (this.state == State.WAIT_SECOND) {
			this.handleSecondEye(tracker);
		}
	}

	/** 第一颗末影之眼：求出第一条直线并等待玩家换位置。 */
	private void handleFirstEye(EyeTracker tracker) {
		Trajectory line = tracker.toTrajectory();

		if (line == null) {
			// 无法解出直线（例如末影之眼在做垂直运动，或者轨迹不是直线）。
			this.fail();
			return;
		}

		if (line.dy() < -0.05D) {
			// y 坐标在减小，说明末影之眼朝下飞：玩家已经在要塞脚下。
			print(Messages.strongholdNearby());
			this.awaitingThrow = true;
			return;
		}

		this.firstLine = line;
		this.firstPlayerX = tracker.playerX;
		this.firstPlayerZ = tracker.playerZ;
		this.state = State.WAIT_SECOND;
		this.awaitingThrow = true;
		print(Messages.throwSecondEye());
	}

	/** 第二颗末影之眼：求出交点并输出要塞坐标。 */
	private void handleSecondEye(EyeTracker tracker) {
		Trajectory second = tracker.toTrajectory();

		if (second == null || this.firstLine == null) {
			this.fail();
			return;
		}

		// 两次扔出末影之眼时玩家所在位置必须至少相距 32 格。
		double playerDx = tracker.playerX - this.firstPlayerX;
		double playerDz = tracker.playerZ - this.firstPlayerZ;

		if (Math.sqrt(playerDx * playerDx + playerDz * playerDz) < MIN_THROW_DISTANCE) {
			print(Messages.needFarther());
			this.awaitingThrow = false;
			this.state = State.WAIT_FAR;
			return;
		}

		Trajectory first = this.firstLine;
		double[] intersection = intersect(first, second);

		if (intersection == null) {
			// 两条直线平行（没有交点）或方向退化：两颗末影之眼指向了不同的要塞。
			this.reset();
			print(Messages.failTooFar());
			return;
		}

		double x = intersection[0];
		double z = intersection[1];
		double dx = x - tracker.playerX;
		double dz = z - tracker.playerZ;

		// 用第一条直线估算要塞的高度，从而按三维直线距离判断是否超过 20000 格。
		double dy = first.dx() == 0.0D ? 0.0D : first.dy() * (dx / first.dx());
		double distance = Math.sqrt(dx * dx + dz * dz + dy * dy);

		if (!Double.isFinite(distance) || distance > MAX_STRONGHOLD_DISTANCE) {
			this.reset();
			print(Messages.failTooFar());
			return;
		}

		long strongholdX = Math.round(x);
		long strongholdZ = Math.round(z);

		this.reset();
		print(Messages.calculated());
		print(Messages.eyeLabel1() + first.display());
		print(Messages.eyeLabel2() + second.display());
		print(Messages.strongholdCoordinates(strongholdX, strongholdZ));
		// 主世界的要塞坐标除以 8 就是下界的对应坐标（与游戏的传送门换算一致）。
		print(Messages.netherCoordinates(strongholdX, strongholdZ));
	}

	/**
	 * 求两条直线的交点。
	 *
	 * @return {@code {x, z}}；两条直线平行或方向退化时返回 null
	 */
	private static double[] intersect(Trajectory a, Trajectory b) {
		double denominator = a.dx() * b.dz() - b.dx() * a.dz();

		if (Math.abs(denominator) < 1.0E-9D) {
			return null;
		}

		double t = ((b.x0() - a.x0()) * b.dz() - (b.z0() - a.z0()) * b.dx()) / denominator;

		return new double[] {a.x0() + t * a.dx(), a.z0() + t * a.dz()};
	}

	/**
	 * 判断玩家半径 16 格内是否有正在运动的末影之眼。
	 *
	 * @param ignoredA 正在被检测的那一颗本身
	 * @param ignoredB 上一颗已经采集完成的末影之眼（可能还在飞，不能算作干扰）
	 */
	private boolean hasMovingEyeNearby(ClientLevel level, LocalPlayer player, EyeOfEnder ignoredA,
			EyeOfEnder ignoredB) {
		for (Entity entity : level.entitiesForRendering()) {
			if (!(entity instanceof EyeOfEnder eye) || eye.isRemoved() || eye == ignoredA || eye == ignoredB) {
				continue;
			}

			if (!isMoving(eye)) {
				continue;
			}

			if (eye.distanceToSqr(player.getX(), player.getY(), player.getZ()) <= INTERFERENCE_RADIUS
					* INTERFERENCE_RADIUS) {
				return true;
			}
		}

		return false;
	}

	/** 玩家附近时候还有别的、还在运动的末影之眼（用于判断干扰是否已经消失）。 */
	private boolean hasAnyOtherMovingEye(ClientLevel level, LocalPlayer player) {
		return this.hasMovingEyeNearby(level, player, null, this.lastCompletedEye);
	}

	private static boolean isMoving(EyeOfEnder eye) {
		return eye.getDeltaMovement().lengthSqr() > MOVING_EPSILON;
	}

	// ------------------------------------------------------------------
	// 状态与提示
	// ------------------------------------------------------------------

	/** 计算失败：提示并退出计算状态。 */
	private void fail() {
		this.reset();
		print(Messages.fail());
	}

	/** 清空本次计算的全部数据并回到休眠状态。 */
	private void reset() {
		this.state = State.OFF;
		this.awaitingThrow = false;
		this.clearRound();
	}

	/** 只清空已经记录到的坐标与直线，保留当前状态。 */
	private void clearRound() {
		this.trackers.clear();
		this.firstLine = null;
		this.firstPlayerX = 0.0D;
		this.firstPlayerZ = 0.0D;
		this.lastCompletedEye = null;
	}

	/** 在聊天栏输出（只在客户端本地显示，不会发送到服务器）。 */
	private static void print(String message) {
		Minecraft client = Minecraft.getInstance();

		if (client.gui != null) {
			client.gui.hud.getChat().addClientSystemMessage(Component.literal(message));
		}
	}
}
