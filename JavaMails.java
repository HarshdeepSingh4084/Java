package others;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.search.FlagTerm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A final class having the methods to get the body of unread emails and also to mark them as read.
 */
final public class JavaMails {

    private final String USERNAME;
    private final String PASSWORD;
    private final String HOST;
    private boolean enableAuthentication;
    private final String PORT;

    private JavaMails() {
        this.USERNAME = null;
        this.PASSWORD = null;
        this.HOST = null;
        this.PORT = null;
    }

    JavaMails(String username, String password, String host, boolean enableAuthentication, String port) {
        this.USERNAME = username;
        this.PASSWORD = password;
        this.HOST = host;
        this.enableAuthentication = enableAuthentication;
        this.PORT = port;
    }

    /**
     * Method to get the {@link IMAPStore} object that has all the methods to make use of IMAP-specific features. This method is using {@link Session} object to connect to an SMTP server and to do that, this method is using {@link Session}'s getDefaultInstance({@link Properties}, {@link Authenticator}) method.
     * @return IMAPStore object
     * @throws MessagingException exception that can be thrown in case of wrong protocol or if there's any problem connecting to the SMTP server
     */
    private IMAPStore getIMAPStore() throws MessagingException {
        //creating a properties object having all the details need to establish a connection to server
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", enableAuthentication);
        properties.put("mail.smtp.host", HOST);
        properties.put("mail.smtp.port", PORT);

        //creating the session object using default instance provider and password authentication
        Session session = Session.getDefaultInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return super.getPasswordAuthentication();
            }
        });

        //creating IMAPStore object using protocol "imaps"
        IMAPStore imapStore = (IMAPStore) session.getStore("imaps");
        imapStore.connect(HOST, USERNAME, PASSWORD);
        return imapStore;
    }

    /**
     * Method to read the unread emails.<br />
     * <strong>Note:<strong/>This function is only handling the message with text/plain body.
     * @return Mail body of all unread emails in the form of String
     * @throws MessagingException
     * @throws IOException
     */
    public String readUnreadMails() throws MessagingException, IOException {
        //getting the IMAPStore object
        IMAPStore imapStore = getIMAPStore();
        if(!imapStore.isConnected())
            throw new MessagingException("IMAPStore is not connected. Please try again."); //throwing exception if IMAPStore is not connected

        IMAPFolder inbox = (IMAPFolder) imapStore.getFolder("inbox"); //accessing the inbox folder
        inbox.open(Folder.READ_WRITE); //opening inbox in read-write mode i.e we can read as well as make changes

        StringBuilder stringBuilder = new StringBuilder();
        Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false)); //fetching all the unread messages
        for (Message message : messages) {
            message.setFlag(Flags.Flag.SEEN, true); //marking the message as read

            Address [] ccAddressesArr = message.getRecipients(Message.RecipientType.CC); //getting all email IDs mentioned in CC
            List<String> ccAddresses = new ArrayList<>();
            if (ccAddressesArr != null) {
                for (Address address : ccAddressesArr)
                    ccAddresses.add(((InternetAddress) address).getAddress());
            }

            //getting the to-addresses
            Address [] toAddressesArr = message.getRecipients(Message.RecipientType.TO); //getting all email IDs mentioned in To
            List<String> toAddresses = new ArrayList<>();
            for (Address address : toAddressesArr)
                toAddresses.add(((InternetAddress) address).getAddress());

            //getting the from address
            Address [] fromAddressArr = message.getFrom(); //getting the email IDs mentioned in From
            List<String> fromAddresses = new ArrayList<>();
            for (Address address : fromAddressArr)
                fromAddresses.add(((InternetAddress) address).getAddress());

            Object messageContent = message.getContent();
            if(messageContent instanceof Multipart) {
                Multipart multipart = (Multipart) message.getContent(); //getting the message content whose part at 0 index is body of the mail
                Part part = multipart.getBodyPart(0);
                String wholeContent = part.getContent().toString();
                //handling only the text mails, not HTML mails
                if (part.isMimeType("text/plain")) {
                    stringBuilder.append("From: ").append(String.join(",", fromAddresses)).append("\n").append("To: ").append(String.join(",", toAddresses)).append("\n");
                    if (!ccAddresses.isEmpty()) stringBuilder.append("CC: ").append(String.join(",", ccAddresses)).append("\n");
                    if (message.getSubject() != null) stringBuilder.append("Subject: ").append(message.getSubject()).append("\n\n");

                    stringBuilder.append(wholeContent);
                    stringBuilder.append("\n\n\n");
                }
            }
        }

        inbox.close();
        imapStore.close();
        return stringBuilder.toString();
    }
}

class Main{
    public static void main(String[] args) {
        JavaMails javaMails = new JavaMails("john.doe@example.com", "*****", "smtp.client.com", true, "000");
        try {
            System.out.println(javaMails.readUnreadMails());
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }
}
