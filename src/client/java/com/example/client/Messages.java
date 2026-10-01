package com.example.client;

/**
 * 当前语言下所有聊天栏提示与输出文本的取值入口。
 *
 * <p>文本本身放在 {@link Language} 里，这里只是按照当前语言取出来，
 * 让 {@link StrongholdSearcher} 之类的调用方不必关心语言细节。</p>
 */
final class Messages {
	private Messages() {
	}

	private static Language lang() {
		return Language.current();
	}

	/** 尚未开始计算，提示玩家扔出第一颗末影之眼。 */
	static String throwFirstEye() {
		return lang().throwFirstEye();
	}

	/** 第一颗记录完成，提示玩家换位置扔第二颗。 */
	static String throwSecondEye() {
		return lang().throwSecondEye();
	}

	/** 周围有其他正在运动的末影之眼，本次获取坐标失败。 */
	static String otherEyesNearby() {
		return lang().otherEyesNearby();
	}

	/** 末影之眼朝下飞：要塞已经在附近。 */
	static String strongholdNearby() {
		return lang().strongholdNearby();
	}

	/** 两次扔出位置不足 32 格。 */
	static String needFarther() {
		return lang().needFarther();
	}

	/** 两条直线平行或交点距离过远。 */
	static String failTooFar() {
		return lang().failTooFar();
	}

	/** 其他无法获取坐标或者无法计算的情况。 */
	static String fail() {
		return lang().fail();
	}

	/** 玩家输入 {@code @stop}。 */
	static String endCalculation() {
		return lang().endCalculation();
	}

	/** 计算成功输出的标题。 */
	static String calculated() {
		return lang().calculated();
	}

	/** 第一条直线的前缀，例如 {@code 末影之眼1   }。 */
	static String eyeLabel1() {
		return lang().eyeLabel1();
	}

	/** 第二条直线的前缀。 */
	static String eyeLabel2() {
		return lang().eyeLabel2();
	}

	/** 主世界要塞坐标的格式：{@code 前缀 + x + 分隔符 + z + 后缀}。 */
	static String strongholdCoordinates(long x, long z) {
		Language language = lang();

		return language.strongholdPrefix() + x + language.strongholdSeparator() + z
				+ language.strongholdSuffix();
	}

	/**
	 * 要塞在主世界坐标除以 8 得到的下界坐标（与游戏传送门换算一致，
	 * 负坐标同样向下取整），格式与主世界坐标一致。
	 */
	static String netherCoordinates(long x, long z) {
		Language language = lang();
		long netherX = Math.floorDiv(x, 8L);
		long netherZ = Math.floorDiv(z, 8L);

		return language.netherPrefix() + netherX + language.strongholdSeparator() + netherZ
				+ language.netherSuffix();
	}

	/** 语言切换成功的提示。 */
	static String languageSwitched() {
		return lang().switched();
	}
}
