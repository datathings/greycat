/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.enterprise.email;

import greycat.Action;
import greycat.Graph;
import greycat.TaskContext;
import greycat.Type;
import greycat.plugin.ActionFactory;
import greycat.plugin.Plugin;
import greycat.struct.Buffer;

import javax.mail.*;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Created by Gregory NAIN on 25/05/2017.
 */
public class EmailPlugin implements Plugin {

    public static final String ACTION_SEND_EMAIL = "sendEmail";

    private String  smtpUser, smtpPass;

    private Properties props;


    public EmailPlugin(String smtpHost, String smtpPort, String smtpUser, String smtpPass, boolean authenticate, boolean starttls) {
        this.smtpUser = smtpUser;
        this.smtpPass = smtpPass;

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
                        return new EmailAction((String)params[0], (String)params[1], (String)params[2], (String)params[3]);
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


            Session session = Session.getInstance(props);
            MimeMessage message = new MimeMessage(session);

            try {
                message.setFrom(new InternetAddress(from));

                message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

                message.setSubject(subject);
                message.setText(body);

                Transport.send(message, smtpUser, smtpPass);

            } catch (MessagingException e) {
                e.printStackTrace();
            }
            ctx.continueTask();
        }

        @Override
        public void serialize(Buffer builder) {

        }

        @Override
        public String name() {
            return ACTION_SEND_EMAIL;
        }
    }


}
