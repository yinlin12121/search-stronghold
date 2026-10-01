package com.example.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.example.client.StrongholdSearcher;

import net.minecraft.client.gui.screens.ChatScreen;

/**
 * 拦截玩家在聊天栏输入的 {@code @start} / {@code @stop} / {@code @language}。
 *
 * <p>这些指令在 {@code ChatScreen.handleChatInput} 的最开头就被取消（{@code ci.cancel()}），
 * 因此根本不会生成聊天数据包，服务器和其他玩家都看不到它们，
 * 多人模式下也可以安全使用。</p>
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
	private static final String START = "@start";
	private static final String STOP = "@stop";
	private static final String LANGUAGE = "@language";

	@Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
	private void searchStronghold$handleChatInput(String message, boolean addToHistory, CallbackInfo ci) {
		boolean start = matches(message, START);
		boolean stop = matches(message, STOP);

		if (start || stop) {
			// 取消原方法：这条消息不会被发送到服务器。
			ci.cancel();

			if (start) {
				StrongholdSearcher.INSTANCE.start();
			} else {
				StrongholdSearcher.INSTANCE.stop();
			}

			return;
		}

		// @language <English|Chinese>：语言名忽略首字母大小写。
		String languageName = argumentOf(message, LANGUAGE);

		if (languageName == null) {
			// 不是 @language 指令（例如 @languageEnglish），当作普通聊天发送。
			return;
		}

		// 只要看上去是 @language 指令就本地拦截：即使语言名写错了（例如 @language French）
		// 或者没写语言名（@language），也不会把这条消息发送到服务器。
		ci.cancel();

		// 语言名无法识别时不做任何处理，也不给提示。
		StrongholdSearcher.INSTANCE.switchLanguage(languageName);
	}

	/**
	 * 只有完全相等，或者指令后面跟着空白（例如 {@code @start }）才算匹配；
	 * 像 {@code @starter} 这样的文本会原样作为普通聊天发送，避免误伤。
	 */
	private static boolean matches(String message, String command) {
		return argumentOf(message, command) != null;
	}

	/**
	 * 取出 {@code @command} 后面的参数。
	 *
	 * @return 参数（可能为空字符串）；没有以该指令开头时返回 null
	 */
	private static String argumentOf(String message, String command) {
		if (message == null) {
			return null;
		}

		String trimmed = message.trim();

		if (trimmed.equals(command)) {
			return "";
		}

		if (trimmed.length() > command.length()
				&& trimmed.startsWith(command)
				&& Character.isWhitespace(trimmed.charAt(command.length()))) {
			return trimmed.substring(command.length()).trim();
		}

		return null;
	}
}
