package com.example.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.client.StrongholdSearcher;

import net.minecraft.client.gui.screens.ChatScreen;

/**
 * 拦截玩家在聊天栏输入的 {@code @start} / {@code @stop}。
 *
 * <p>这两个指令在 {@code ChatScreen.handleChatInput} 的最开头就被取消（{@code ci.cancel()}），
 * 因此根本不会生成聊天数据包，服务器和其他玩家都看不到它们，
 * 多人模式下也可以安全使用。</p>
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	private static final String START = "@start";
	private static final String STOP = "@stop";

	@Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
	private void searchStronghold$handleChatInput(String message, boolean addToHistory, CallbackInfo ci) {
		boolean start = matches(message, START);
		boolean stop = matches(message, STOP);

		if (!start && !stop) {
			return;
		}

		// 取消原方法：这条消息不会被发送到服务器。
		ci.cancel();

		if (start) {
			StrongholdSearcher.INSTANCE.start();
		} else {
			StrongholdSearcher.INSTANCE.stop();
		}
	}

	/**
	 * 只有完全相等，或者指令后面跟着空白（例如 {@code @start }）才算匹配；
	 * 像 {@code @starter} 这样的文本会原样作为普通聊天发送，避免误伤。
	 */
	private static boolean matches(String message, String command) {
		if (message == null) {
			return false;
		}

		String trimmed = message.trim();

		if (trimmed.equals(command)) {
			return true;
		}

		return trimmed.length() > command.length()
				&& trimmed.startsWith(command)
				&& Character.isWhitespace(trimmed.charAt(command.length()));
	}
}
