package com.spingbootinit.utils;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;

/**
 * 短信发送工具类
 */
public class SMSUtils {
    // 签名
    private final static String SIGN_NAME = "XXXX";
    // 模板
    private final static String TEMPLATE_CODE = "XXXX";

    /**
     * 发送短信
     *
     * @param phoneNumbers 收信人手机号
     * @param param        发送的验证码
     */
    public static void sendMessage(String phoneNumbers, String param) {
        // 配置的accessKeyId和secret
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", "xxxx", "xxxxxx");
        IAcsClient client = new DefaultAcsClient(profile);

        SendSmsRequest request = new SendSmsRequest();
        request.setSysRegionId("cn-hangzhou");
        // 收信人手机号
        request.setPhoneNumbers(phoneNumbers);
        // 申请的签名
        request.setSignName(SIGN_NAME);
        // 申请的模板
        request.setTemplateCode(TEMPLATE_CODE);
        // 替换模板中的参数，必须为Json格式
        request.setTemplateParam("{\"code\":\"" + param + "\"}");
        try {
            // 获取发送结果
            SendSmsResponse response = client.getAcsResponse(request);
            System.out.println(response);
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            // 打印处理结果
            System.out.println("ErrCode:" + e.getErrCode());
            System.out.println("ErrMsg:" + e.getErrMsg());
            System.out.println("RequestId:" + e.getRequestId());
        }
    }

}
