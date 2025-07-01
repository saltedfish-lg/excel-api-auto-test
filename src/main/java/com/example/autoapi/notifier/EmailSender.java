package com.example.autoapi.notifier;

import com.example.autoapi.config.EnvConfig;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);

    public static void sendReport(String reportPath) {
        if (!"true".equalsIgnoreCase(EnvConfig.get("mail.enabled"))) return;

        String host = EnvConfig.get("mail.host");
        String port = EnvConfig.get("mail.port");
        String username = EnvConfig.get("mail.username");
        String password = EnvConfig.get("mail.password");
        String to = EnvConfig.get("mail.to");
        String subject = EnvConfig.get("mail.subject");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("API自动化测试执行完成，报告见附件。");

            MimeBodyPart attachmentPart = new MimeBodyPart();
            File file = new File(reportPath);
            DataSource source = new FileDataSource(file);
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(file.getName());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);
            Transport.send(message);
            logger.info("✅ 报告邮件发送成功");

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("❌ 报告邮件发送失败: {}", e.getMessage());
        }
    }
}
