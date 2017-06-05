/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.enterprise.email;

import com.sun.mail.smtp.SMTPTransport;
import greycat.*;
import greycat.internal.task.TaskHelper;
import greycat.plugin.ActionFactory;
import greycat.plugin.Plugin;
import greycat.struct.Buffer;
import greycat.utility.Base64;

import javax.mail.*;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by Gregory NAIN on 25/05/2017.
 */
public class EmailPlugin implements Plugin {

    public static final String ACTION_SEND_EMAIL = "sendEmail";

    private String smtpUser, smtpPass, smtpHost;
    private int smtpPort;

    private Properties props;


    public EmailPlugin(String smtpHost, String smtpPort, String smtpUser, String smtpPass, boolean authenticate, boolean starttls) {
        this.smtpUser = smtpUser;
        this.smtpPass = smtpPass;
        this.smtpHost = smtpHost;
        this.smtpPort = Integer.valueOf(smtpPort);

        this.props = new Properties();
        props.put("mail.smtp.starttls.enable", (starttls ? "true" : "false"));
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.user", smtpUser);
        props.put("mail.smtp.password", smtpPass);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", (authenticate ? "true" : "false"));

    }

    @Override
    public void start(Graph graph) {
        graph.actionRegistry().getOrCreateDeclaration(ACTION_SEND_EMAIL)
                .setParams(Type.STRING, Type.STRING, Type.STRING, Type.STRING, Type.STRING)
                .setFactory(new ActionFactory() {
                    @Override
                    public Action create(Object[] params) {
                        return new EmailAction((String) params[0], (String) params[1], (String) params[2], (String) params[3]);
                    }
                });

    }

    @Override
    public void stop() {

    }

    class EmailAction implements Action {

        private String _from, _to, _subject, _body;

        public EmailAction(String from, String to, String subject, String body) {
            this._from = from;
            this._to = to;
            this._subject = subject;
            this._body = body;
        }

        @Override
        public void eval(TaskContext ctx) {

            String from = ctx.template(_from);
            String to = ctx.template(_to);
            String subject = ctx.template(_subject);
            String body = ctx.template(_body);

            try {

                Session session = Session.getInstance(props);

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
                message.setSubject(subject);
                message.setText(body);

                SMTPTransport transport = (SMTPTransport) session.getTransport("smtp");

                transport.addTransportListener(new TransportListener() {
                    @Override
                    public void messageDelivered(TransportEvent transportEvent) {
                        System.out.println("Message delivered to " + Arrays.toString(transportEvent.getValidSentAddresses()));
                        ctx.append("Email sent to " + Arrays.toString(transportEvent.getValidSentAddresses()));
                        ctx.continueTask();
                    }

                    @Override
                    public void messageNotDelivered(TransportEvent transportEvent) {
                        System.out.println("Message Not delivered to " + Arrays.toString(transportEvent.getValidUnsentAddresses()));
                        ctx.endTask(null, new Exception("Email not sent to " + Arrays.toString(transportEvent.getValidUnsentAddresses())+"\n" +transport.getLastServerResponse()));
                    }

                    @Override
                    public void messagePartiallyDelivered(TransportEvent transportEvent) {
                        System.out.println("Message delivered to " + Arrays.toString(transportEvent.getValidSentAddresses()));
                        System.out.println("Message Not delivered to " + Arrays.toString(transportEvent.getInvalidAddresses()));
                        ctx.endTask(null, new Exception("Email sent to " + Arrays.toString(transportEvent.getValidSentAddresses()) + "\n" + "Email not sent to " + Arrays.toString(transportEvent.getValidUnsentAddresses())+"\n" +transport.getLastServerResponse()));
                    }
                });
                transport.setRequireStartTLS(true);
                transport.setStartTLS(true);
                ctx.reportProgress(1,3,"Connection to email server");
                transport.connect(smtpHost, smtpPort, smtpUser, smtpPass);
                ctx.reportProgress(2,3,"Sending message");
                try {
                    transport.sendMessage(message, message.getAllRecipients());
                } catch(SendFailedException e) {
                    ctx.append(e.getMessage());
                }
                transport.close();
                ctx.reportProgress(3,3,"Message sent");

            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void serialize(Buffer buffer) {
            buffer.writeString(ACTION_SEND_EMAIL);
            buffer.writeChar(Constants.TASK_PARAM_OPEN);
            TaskHelper.serializeString(_from, buffer, true);
            buffer.writeChar(Constants.TASK_PARAM_SEP);
            TaskHelper.serializeString(_to, buffer, true);
            buffer.writeChar(Constants.TASK_PARAM_SEP);
            TaskHelper.serializeString(_subject, buffer, true);
            buffer.writeChar(Constants.TASK_PARAM_SEP);
            Base64.encodeStringToBuffer(_body, buffer);
            buffer.writeChar(Constants.TASK_PARAM_CLOSE);
        }

        @Override
        public String name() {
            return ACTION_SEND_EMAIL;
        }
    }


}
