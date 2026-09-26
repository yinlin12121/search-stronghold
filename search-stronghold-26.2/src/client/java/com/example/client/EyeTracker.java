package com.example.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

/**
 * 末影之眼轨迹记录器。
 *
 * <p>每只被观测到的末影之眼会在这里记录若干个时刻的 {@code (x, y, z)} 坐标
 * （double 精度），最后由其中相隔最远的两点得到这条线段所在的直线。</p>
 */
final class EyeTracker {
	/** 抛出时玩家所在位置（用于 32 格距离校验与后续的距离判断）。 */
	final double playerX;
	final double playerY;
	final double playerZ;

	/** 采样点数：取 3 个点，保证有足够的基线长度，同时可以校验轨迹是否是一条直线。 */
	private static final int MAX_SAMPLES = 3;
	/** 每隔若干个游戏刻采一次样（约 1~2 格：既保证基线足够长，也保证还在飞行阶段）。 */
	private static final int SAMPLE_INTERVAL = 2;
	/** 认为末影之眼确实在运动的阈值。 */
	private static final double MOVING_EPSILON = 0.01D;
	/** 三个采样点偏离同一条直线超过这个距离，就认为记录被干扰了。 */
	private static final double COLLINEAR_TOLERANCE = 0.05D;

	/** 观测到的采样点，按时间顺序排列。 */
	private final double[] xs = new double[MAX_SAMPLES];
	private final double[] ys = new double[MAX_SAMPLES];
	private final double[] zs = new double[MAX_SAMPLES];
	private int sampleCount;
	private int cooldown;

	private boolean finished;

	EyeTracker(LocalPlayer player) {
		this.playerX = player.getX();
		this.playerY = player.getY();
		this.playerZ = player.getZ();
	}

	/**
	 * 采样一次。
	 *
	 * @return true 表示这只末影之眼已经可以结算（采够数据，或者已经落地）
	 */
	boolean sample(Entity eye) {
		if (this.finished) {
			return false;
		}

		double x = eye.getX();
		double y = eye.getY();
		double z = eye.getZ();

		// 末影之眼已经落地：继续跟踪没有意义，也没有拿到有效数据。
		if (eye.onGround()) {
			this.finished = true;
			return true;
		}

		// 还没开始运动（速度太小）时不采样，避免把刚生成的静止帧当成轨迹。
		if (eye.getDeltaMovement().lengthSqr() < MOVING_EPSILON * MOVING_EPSILON) {
			return false;
		}

		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}

		this.cooldown = SAMPLE_INTERVAL;

		if (this.sampleCount < MAX_SAMPLES) {
			this.xs[this.sampleCount] = x;
			this.ys[this.sampleCount] = y;
			this.zs[this.sampleCount] = z;
			this.sampleCount++;
		}

		if (this.sampleCount >= MAX_SAMPLES) {
			this.finished = true;
			return true;
		}

		return false;
	}

	/** 是否已经采到足够的数据（至少两个不同时刻的坐标）。 */
	boolean hasEnoughSamples() {
		return this.sampleCount >= 2;
	}

	/**
	 * 由本次记录的坐标求出末影之眼的运动直线。
	 *
	 * <p>末影之眼的运动路线从平面（xz）上看是一条线段，因此取其中相隔最远的两点
	 * （首尾两点）即可得到该直线的一次函数表达式 {@code z = kx + b}。</p>
	 *
	 * @return 直线；若数据不足、轨迹退化或者轨迹不是一条直线则返回 null
	 */
	Trajectory toTrajectory() {
		if (this.sampleCount < 2) {
			return null;
		}

		// 首尾两点：基线最长，相对误差最小。
		double x1 = this.xs[0];
		double y1 = this.ys[0];
		double z1 = this.zs[0];
		double x2 = this.xs[this.sampleCount - 1];
		double y2 = this.ys[this.sampleCount - 1];
		double z2 = this.zs[this.sampleCount - 1];

		double dx = x2 - x1;
		double dz = z2 - z1;
		double horizontal = Math.hypot(dx, dz);

		// 水平方向几乎没有位移：无法确定直线（例如末影之眼朝正上方飞）。
		if (horizontal < 0.05D) {
			return null;
		}

		// 校验所有采样点是否落在同一条直线上：末影之眼在平面上走的是直线，
		// 如果中间的点偏离这条直线，说明记录被干扰了，直接判定为计算失败。
		for (int i = 1; i < this.sampleCount - 1; i++) {
			double cross = (this.xs[i] - x1) * dz - (this.zs[i] - z1) * dx;

			if (Math.abs(cross) / horizontal > COLLINEAR_TOLERANCE) {
				return null;
			}
		}

		return new Trajectory(x1, y1, z1, dx, dz, y2 - y1);
	}
}
