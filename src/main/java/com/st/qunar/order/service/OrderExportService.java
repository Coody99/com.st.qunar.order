/**
 * 
 */
package com.st.qunar.order.service;

import java.io.IOException;

import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springside.modules.mapper.JaxbMapper;

import com.st.qunar.order.entity.Result;

/**
 * @author kxhu
 * 
 */
@Component
public class OrderExportService implements Runnable {

	@SuppressWarnings("unused")
	private static final int TIMEOUT_SECONDS = 20;

	private static final String orderExportUrl = "http://ygz.trade.qunar.com/tts/interface/new/orderexport?";
	private static final String orderExportIncrParaUserAndPass = "user=ygQunarTtsOrder&pass=K39dKd@8";
	private static final String orderExportIncrParaType = "type=incr";
	private static final String orderExportIncrParaLastId = "lastId=";

	private static Logger logger = LoggerFactory.getLogger(OrderExportService.class);

	@Autowired
	private OrderService orderService;

	@Autowired
	private CommonCountService commonCountService;

	@Override
	public void run() {
		String threadName = Thread.currentThread().getName();
		System.out.println("import qunar order running" + " ThreadName:" + threadName);
		while (true) {
			try {
				// 增量导出
				String reqUrl = orderExportUrl
						+ orderExportIncrParaUserAndPass
						+ "&"
						+ orderExportIncrParaType
						+ "&"
						+ orderExportIncrParaLastId
						+ commonCountService
								.getCommonCountByTypeName(CommonCountService.COMM_COUNT_QN_ORDER_INCR_EXP_LAS_ID);
				String exportContent = Request.Post(reqUrl).execute().returnContent().asString();
				// 导出结果xml转为对象
				Result result = JaxbMapper.fromXml(exportContent, Result.class);
				if (!result.getStatus().equals("ok")) {
					String exErrContent = result.getMsg().getContent();
					logger.error("export content error:", exErrContent);
				} else {
					orderService.saveOrders(result.getOrders());
					Long lastId = result.getOrders().get(result.getOrders().size() - 1).getOrderId();
					commonCountService.updateCountByTypeName(CommonCountService.COMM_COUNT_QN_ORDER_INCR_EXP_LAS_ID,
							lastId);
				}
			} catch (IOException e) {
				logger.error("export post request error", e);
				e.printStackTrace();
			}

			try {
				Thread.sleep(5 * 60 * 1000);
			} catch (InterruptedException e) {
				logger.error("import order thread sleep 5mins error", e);
				e.printStackTrace();
			}
		}
	}
}