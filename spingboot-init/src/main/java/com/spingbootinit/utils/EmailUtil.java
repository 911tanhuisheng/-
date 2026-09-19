package com.spingbootinit.utils;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 邮件工具类
 */
@Component
public class EmailUtil {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送验证码邮件
     */
    public void sendCode(String to, String code) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject("熊猫外卖 - 验证码");

        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>您的 Steam 帐户：来自新电脑的访问</title>\n" +
                "    <style>\n" +
                "        * {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            box-sizing: border-box;\n" +
                "        }\n" +
                "        body {\n" +
                "            font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Microsoft YaHei\", sans-serif;\n" +
                "            background-color: #121212;\n" +
                "            color: #e0e0e0;\n" +
                "        }\n" +
                "        .email-container {\n" +
                "            max-width: 600px;\n" +
                "            margin: 0 auto;\n" +
                "            background-color: #1e1e1e;\n" +
                "            overflow: hidden;\n" +
                "        }\n" +
                "        .email-header {\n" +
                "            padding: 20px;\n" +
                "            border-bottom: 1px solid #333;\n" +
                "        }\n" +
                "        .header-top {\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            align-items: center;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .header-title {\n" +
                "            font-size: 22px;\n" +
                "            font-weight: 500;\n" +
                "            color: #ffffff;\n" +
                "        }\n" +
                "        .header-sub {\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            gap: 10px;\n" +
                "            font-size: 14px;\n" +
                "            color: #aaa;\n" +
                "        }\n" +
                "        .steam-avatar {\n" +
                "            width: 40px;\n" +
                "            height: 40px;\n" +
                "            background-color: #171a21;\n" +
                "            border-radius: 50%;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        .steam-avatar svg {\n" +
                "            width: 24px;\n" +
                "            height: 24px;\n" +
                "            fill: #c6d4df;\n" +
                "        }\n" +
                "        .username {\n" +
                "            font-size: 36px;\n" +
                "            color: #66c0f4;\n" +
                "            margin: 10px 0 20px;\n" +
                "        }\n" +
                "        .intro-text {\n" +
                "            font-size: 16px;\n" +
                "            line-height: 1.6;\n" +
                "            color: #c6d4df;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .code-box {\n" +
                "            background-color: #121212;\n" +
                "            padding: 25px;\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 30px;\n" +
                "        }\n" +
                "        .code-label {\n" +
                "            font-size: 18px;\n" +
                "            color: #aaa;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .code-region {\n" +
                "            font-size: 28px;\n" +
                "            color: #eaeaea;\n" +
                "            margin-bottom: 10px;\n" +
                "        }\n" +
                "        .code {\n" +
                "            font-size: 48px;\n" +
                "            font-weight: bold;\n" +
                "            color: #66c0f4;\n" +
                "            letter-spacing: 4px;\n" +
                "        }\n" +
                "        .section-title {\n" +
                "            font-size: 32px;\n" +
                "            font-weight: 500;\n" +
                "            color: #eaeaea;\n" +
                "            margin: 30px 0 15px;\n" +
                "        }\n" +
                "        .section-text {\n" +
                "            font-size: 16px;\n" +
                "            line-height: 1.6;\n" +
                "            color: #c6d4df;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        .section-text a {\n" +
                "            color: #66c0f4;\n" +
                "            text-decoration: underline;\n" +
                "        }\n" +
                "        .footer-signature {\n" +
                "            margin: 20px 0;\n" +
                "            padding-left: 15px;\n" +
                "            border-left: 3px solid #66c0f4;\n" +
                "        }\n" +
                "        .handwritten {\n" +
                "            font-family: \"Comic Sans MS\", cursive, sans-serif;\n" +
                "            font-size: 14px;\n" +
                "            color: #c6d4df;\n" +
                "            line-height: 1.8;\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        .footer-links {\n" +
                "            margin: 20px 0;\n" +
                "        }\n" +
                "        .footer-links a {\n" +
                "            color: #c6d4df;\n" +
                "            text-decoration: none;\n" +
                "        }\n" +
                "        .footer-logo {\n" +
                "            margin: 30px 0;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .footer-logo svg {\n" +
                "            width: 120px;\n" +
                "            height: 60px;\n" +
                "            fill: #c6d4df;\n" +
                "        }\n" +
                "        .valve-logo {\n" +
                "            margin: 20px auto;\n" +
                "            text-align: center;\n" +
                "        }\n" +
                "        .valve-logo svg {\n" +
                "            width: 200px;\n" +
                "            height: 80px;\n" +
                "            fill: #c6d4df;\n" +
                "        }\n" +
                "        .copyright {\n" +
                "            font-size: 12px;\n" +
                "            color: #666;\n" +
                "            line-height: 1.5;\n" +
                "            margin-top: 20px;\n" +
                "        }\n" +
                "        /* 移动端适配 */\n" +
                "        @media (max-width: 600px) {\n" +
                "            .email-container {\n" +
                "                margin: 10px;\n" +
                "            }\n" +
                "            .username {\n" +
                "                font-size: 28px;\n" +
                "            }\n" +
                "            .code {\n" +
                "                font-size: 36px;\n" +
                "            }\n" +
                "            .section-title {\n" +
                "                font-size: 24px;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"email-container\">\n" +
                "        <div class=\"email-header\">\n" +
                "            <div class=\"header-top\">\n" +
                "                <h1 class=\"header-title\">您的 Steam 帐户：来自新电脑的访问</h1>\n" +
                "            </div>\n" +
                "            <div class=\"header-sub\">\n" +
                "                <div class=\"steam-avatar\">\n" +
                "                    <span style=\"color: #66c0f4; font-weight: bold;\">ST</span>\n" +
                "                </div>\n" +
                "                <span>Steam 客服</span>\n" +
                "                <span style=\"margin-left: auto;\">2025/08/05</span>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div class=\"email-body\" style=\"padding: 20px;\">\n" +
                "            <div class=\"steam-avatar\">\n" +
                "                <svg viewBox=\"0 0 1024 1024\">\n" +
                "                    <path d=\"M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z\"/>\n" +
                "                    <path d=\"M464 336a48 48 0 1 0 96 0 48 48 0 1 0-96 0zm72 112c-88.4 0-160 71.6-160 160 0 88.4 71.6 160 160 160s160-71.6 160-160c0-88.4-71.6-160-160-160zm0 256c-53 0-96-43-96-96s43-96 96-96 96 43 96 96-43 96-96 96z\"/>\n" +
                "                </svg>\n" +
                "            </div>\n" +
                "            <h2 class=\"username\">ta12345653，</h2>\n" +
                "            <p class=\"intro-text\">\n" +
                "                看起来您正在尝试使用新设备登录。此处是您访问帐户所需的 Steam 令牌验证码：\n" +
                "            </p>\n" +
                "            <div class=\"code-box\">\n" +
                "                <p class=\"code-label\">请求来自</p>\n" +
                "                <p class=\"code-region\">中国</p>\n" +
                "                <p class=\"code\">C3KX3</p>\n" +
                "            </div>\n" +
                "            <h3 class=\"section-title\">不是您？</h3>\n" +
                "            <p class=\"section-text\">\n" +
                "                您会收到这封电子邮件，是由于有人试图登录您的 Steam 帐户，且提供了<strong>正确的帐户名称与密码</strong>。\n" +
                "            </p>\n" +
                "            <p class=\"section-text\">\n" +
                "                如果这不是您尝试登录，建议您<a href=\"#\">重置自己的 Steam 密码</a>。\n" +
                "            </p>\n" +
                "            <p class=\"section-text\">\n" +
                "                此电子邮件包含一个登录代码，您需要用它访问您的帐户。切勿与任何人分享此代码。\n" +
                "            </p>\n" +
                "            <h3 class=\"section-title\">不住在中国？</h3>\n" +
                "            <p class=\"section-text\">\n" +
                "                如果您不认得上述位置，则可能登录的是虚假 Steam 网站。请先<a href=\"#\">验证您的位置</a>再继续。\n" +
                "            </p>\n" +
                "            <div class=\"footer-signature\">\n" +
                "                <p>祝您愉快，</p>\n" +
                "                <p>Steam 团队</p>\n" +
                "            </div>\n" +
                "            <div class=\"handwritten\">\n" +
                "                <p>此通知已发送至与您的 Steam 帐户关联的电子邮件地址。</p>\n" +
                "                <p>这封电子邮件由系统自动生成，请勿回复。如果您需要额外帮助，请访问 Steam 客服。</p>\n" +
                "            </div>\n" +
                "            <div class=\"footer-links\">\n" +
                "                <a href=\"https://help.steampowered.com\">https://help.steampowered.com</a>\n" +
                "            </div>\n" +
                "            <div class=\"footer-logo\">\n" +
                "                <svg viewBox=\"0 0 200 60\">\n" +
                "                    <path fill=\"#c6d4df\" d=\"M30.9 30.1c0-6.7 3.7-10.4 10.1-10.4 2.8 0 5.2.9 7.1 2.7V9.9c-2-.9-4.3-1.4-6.9-1.4-9.4 0-16.2 7.3-16.2 17.1 0 9.7 6.8 17.1 16.2 17.1 2.6 0 4.9-.5 6.9-1.4V38c-1.9 1.8-4.3 2.7-7.1 2.7-6.4 0-10.1-3.8-10.1-10.6zm39.6-10.2h9.9v27.4h-9.9V19.9zm14.8 0h8.1l11 20.1 11-20.1h8.1v27.4h-9.9V27.9l-9.2 19.4h-9.1l-9.2-19.4v19.4h-9.8V19.9zm59.3 0h9.9v9.3h14.3V19.9h9.9v27.4h-9.9v-8.2H114.6v8.2h-9.9V19.9zm24.7 0h9.9v27.4h-9.9V19.9zm19.8 0h8.1l11 20.1 11-20.1h8.1v27.4h-9.9V27.9l-9.2 19.4h-9.1l-9.2-19.4v19.4h-9.8V19.9z\"/>\n" +
                "                </svg>\n" +
                "            </div>\n" +
                "            <div class=\"valve-logo\">\n" +
                "                <svg viewBox=\"0 0 400 120\">\n" +
                "                    <path fill=\"#c6d4df\" d=\"M50 20h40v80H50V20zm10 10v60h20V30H60zm80 0h20v20h20V30h20v80h-20V70h-20v40h-20V30zm80 0h20v20h20V30h20v80h-20V70h-20v40h-20V30zm80 0h20v20h20V30h20v80h-20V70h-20v40h-20V30zm80 0h20v20h20V30h20v80h-20V70h-20v40h-20V30z\"/>\n" +
                "                </svg>\n" +
                "            </div>\n" +
                "            <div class=\"copyright\">\n" +
                "                <p>© Valve Corporation</p>\n" +
                "                <p>PO Box 1688 Bellevue, WA 98009</p>\n" +
                "                <p>保留所有权利。所有商标均为其在美国及其他国家/地区的各自持有者所有。</p>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>"+code;

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}

