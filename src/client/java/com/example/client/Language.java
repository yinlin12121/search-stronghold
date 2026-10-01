package com.example.client;

/**
 * 模组支持的语言，以及每种语言对应的全部提示文本。
 *
 * <p>玩家可以在聊天栏输入 {@code @language English} 或 {@code @language Chinese}
 * 切换语言；这条指令和 {@code @start} / {@code @stop} 一样只在本地被拦截，
 * 不会发送到服务器。</p>
 */
enum Language {
	/** 中文（默认）。 */
	CHINESE("Chinese",
			"扔出第一颗末影之眼",
			"更换位置，扔出第二个末影之眼",
			"获取坐标失败，周围有其他末影之眼",
			"要塞已在附近",
			"需前往更远的距离（32格之外）",
			"计算失败，要塞距离过远",
			"计算失败",
			"结束计算",
			"已计算",
			"末影之眼1   ",
			"末影之眼2   ",
			"要塞的坐标 （",
			"，",
			"）",
			"要塞在下界的对应坐标（",
			"）",
			"语言已切换为中文"),

	/** 英文。 */
	ENGLISH("English",
			"Throw the first eye of ender",
			"Change position and throw the second eye of ender",
			"Failed to get coordinates; there are other eyes of ender nearby",
			"The stronghold is already nearby",
			"Need to go farther away (beyond 32 blocks)",
			"Calculation failed; the stronghold is too far away",
			"Calculation failed",
			"End calculation",
			"Calculated",
			"Eye of Ender 1   ",
			"Eye of Ender 2   ",
			"Stronghold coordinates (",
			", ",
			")",
			"Stronghold coordinates in the nether (",
			")",
			"Language has been switched to English.");

	/** 当前语言，默认中文。 */
	private static Language current = CHINESE;

	/** 玩家输入的语言名（{@code @language} 后面那一段）。 */
	private final String commandName;
	private final String throwFirstEye;
	private final String throwSecondEye;
	private final String otherEyesNearby;
	private final String strongholdNearby;
	private final String needFarther;
	private final String failTooFar;
	private final String fail;
	private final String endCalculation;
	private final String calculated;
	private final String eyeLabel1;
	private final String eyeLabel2;
	private final String strongholdPrefix;
	private final String strongholdSeparator;
	private final String strongholdSuffix;
	private final String netherPrefix;
	private final String netherSuffix;
	private final String switched;

	Language(String commandName, String throwFirstEye, String throwSecondEye, String otherEyesNearby,
			String strongholdNearby, String needFarther, String failTooFar, String fail, String endCalculation,
			String calculated, String eyeLabel1, String eyeLabel2, String strongholdPrefix,
			String strongholdSeparator, String strongholdSuffix, String netherPrefix, String netherSuffix,
			String switched) {
		this.commandName = commandName;
		this.throwFirstEye = throwFirstEye;
		this.throwSecondEye = throwSecondEye;
		this.otherEyesNearby = otherEyesNearby;
		this.strongholdNearby = strongholdNearby;
		this.needFarther = needFarther;
		this.failTooFar = failTooFar;
		this.fail = fail;
		this.endCalculation = endCalculation;
		this.calculated = calculated;
		this.eyeLabel1 = eyeLabel1;
		this.eyeLabel2 = eyeLabel2;
		this.strongholdPrefix = strongholdPrefix;
		this.strongholdSeparator = strongholdSeparator;
		this.strongholdSuffix = strongholdSuffix;
		this.netherPrefix = netherPrefix;
		this.netherSuffix = netherSuffix;
		this.switched = switched;
	}

	/** 当前语言。 */
	static Language current() {
		return current;
	}

	/** 切换当前语言。 */
	static void setCurrent(Language language) {
		current = language;
	}

	/**
	 * 把玩家输入的语言名解析成语言，忽略首字母大小写
	 * （{@code english}、{@code ENGLISH}、{@code English} 都可以）。
	 *
	 * @return 对应的语言；无法识别时返回 null
	 */
	static Language parse(String name) {
		if (name == null) {
			return null;
		}

		String trimmed = name.trim();

		for (Language language : values()) {
			if (language.commandName.equalsIgnoreCase(trimmed)) {
				return language;
			}
		}

		return null;
	}

	String throwFirstEye() {
		return this.throwFirstEye;
	}

	String throwSecondEye() {
		return this.throwSecondEye;
	}

	String otherEyesNearby() {
		return this.otherEyesNearby;
	}

	String strongholdNearby() {
		return this.strongholdNearby;
	}

	String needFarther() {
		return this.needFarther;
	}

	String failTooFar() {
		return this.failTooFar;
	}

	String fail() {
		return this.fail;
	}

	String endCalculation() {
		return this.endCalculation;
	}

	String calculated() {
		return this.calculated;
	}

	String eyeLabel1() {
		return this.eyeLabel1;
	}

	String eyeLabel2() {
		return this.eyeLabel2;
	}

	String strongholdPrefix() {
		return this.strongholdPrefix;
	}

	String strongholdSeparator() {
		return this.strongholdSeparator;
	}

	String strongholdSuffix() {
		return this.strongholdSuffix;
	}

	String netherPrefix() {
		return this.netherPrefix;
	}

	String netherSuffix() {
		return this.netherSuffix;
	}

	/** 切换成功后的提示。 */
	String switched() {
		return this.switched;
	}
}
