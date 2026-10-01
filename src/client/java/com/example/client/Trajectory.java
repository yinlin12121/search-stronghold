package com.example.client;

/**
 * 一条末影之眼运动路线所在的直线。
 *
 * <p>直线由一点 {@code (x0, z0)} 和方向向量 {@code (dx, dz)} 表示，
 * 其一次函数表达式为 {@code z = kx + b}，其中 {@code k = dz / dx}、
 * {@code b = z0 - k * x0}。</p>
 *
 * <p>{@code (x0, y0, z0)} 是末影之眼运动过程中的一个实测点，
 * {@code dy} 是同一段位移的 y 分量，用于估算要塞的高度。</p>
 */
final class Trajectory {
	private final double x0;
	private final double y0;
	private final double z0;
	private final double dx;
	private final double dz;
	private final double dy;

	Trajectory(double x0, double y0, double z0, double dx, double dz, double dy) {
		this.x0 = x0;
		this.y0 = y0;
		this.z0 = z0;
		this.dx = dx;
		this.dz = dz;
		this.dy = dy;
	}

	double x0() {
		return this.x0;
	}

	double y0() {
		return this.y0;
	}

	double z0() {
		return this.z0;
	}

	double dx() {
		return this.dx;
	}

	double dz() {
		return this.dz;
	}

	double dy() {
		return this.dy;
	}

	/** 斜率 k，保留 double 精度（不做任何取整，以免放大计算误差）。 */
	double k() {
		return this.dz / this.dx;
	}

	/** 截距 b，保留 double 精度。 */
	double b() {
		return this.z0 - this.k() * this.x0;
	}

	/** 保留 1 位小数的数值，仅用于聊天栏显示。 */
	static String format1(double value) {
		return String.valueOf(Math.round(value * 10.0D) / 10.0D);
	}

	/** 用于聊天栏显示的一次函数表达式，例如 {@code z=0.3x+125.4}。 */
	String display() {
		double k = this.k();
		double b = this.b();
		String sign = b < 0.0D ? "-" : "+";

		return "z=" + format1(k) + "x" + sign + format1(Math.abs(b));
	}

	@Override
	public String toString() {
		return this.display();
	}
}
