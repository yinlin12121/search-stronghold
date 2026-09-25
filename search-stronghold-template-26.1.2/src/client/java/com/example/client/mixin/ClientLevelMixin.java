package com.example.client.mixin;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.client.StrongholdSearcher;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;

/**
 * 末影之眼检测的驱动点，以及退出世界时的清理点。
 *
 * <p>每个客户端刻在客户端世界 tick 结束时检查一次末影之眼的运动情况；休眠状态下
 * {@link StrongholdSearcher#tick} 会立刻返回，因此不会读取任何坐标，
 * 也不会影响玩家的任何游戏行为。</p>
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {
	/** 客户端世界 tick 之后：检查末影之眼。 */
	@Inject(method = "tick", at = @At("TAIL"))
	private void searchStronghold$afterTick(BooleanSupplier haveTime, CallbackInfo ci) {
		StrongholdSearcher.INSTANCE.tick(Minecraft.getInstance());
	}

	/**
	 * 玩家离开世界/退出游戏时（包括单人世界退出、多人服务器断开），
	 * 自动退出计算状态，不保存计算结果，也不保存已经记录的坐标。
	 */
	@Inject(method = "disconnect", at = @At("HEAD"))
	private void searchStronghold$onDisconnect(Component message, CallbackInfo ci) {
		StrongholdSearcher.INSTANCE.finish();
	}
}
