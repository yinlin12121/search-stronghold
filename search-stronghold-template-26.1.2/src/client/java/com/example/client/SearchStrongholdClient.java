package com.example.client;

import net.fabricmc.api.ClientModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端入口。
 *
 * <p>本模组完全运行在客户端：所有坐标计算、聊天拦截和提示都只在本地进行，
 * 不向服务器发送任何数据包，因此在多人服务器中可以正常使用。</p>
 */
public class SearchStrongholdClient implements ClientModInitializer {
	public static final String MOD_ID = "search-stronghold";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		// 本模组不需要注册任何物品/方块/数据包：
		// 聊天栏指令由 ChatScreenMixin 拦截，每一客户端刻的检测由 ClientLevelMixin 驱动。
		LOGGER.info("search stronghold 已加载：在聊天栏输入 @start 开始计算，@stop 结束计算。");
	}
}
