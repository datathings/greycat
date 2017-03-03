package greycat;

import javax.crypto.Cipher;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

/**
 * Created by assaad on 03/03/2017.
 */
public class Validator {
    private static final String publicKey = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEfv43+8mAmohQHBg4Bz1eW+1Nf2Ukkwar05q3NxMD8tRDtvC8UZKIBDwPIszpBAa8LF8zcs3aM79/3wt3ULOWaQ==";
    private static final String publicKeyRSA = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKMyb2A7wWaaInUpUP2eV6SC0BJd3U1S/7t9PcxAhHkldZJ18uAI3EnDKA5xJwSrKfCkm5UktgbEh6I5yVjyGIcCAwEAAQ==";
    private static final String delimitter = "---";


    private static boolean validateDate(String date) {
        try {
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            Date expirydate = df.parse(date);
            Date now = new Date();
            return (expirydate.compareTo(now) > 0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private static String getMacAddress() {
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            InetAddress ip;
            ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();
            return encoder.encodeToString(mac);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public static boolean validate() {

        File dir = new File(".");
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".ldt");
            }
        });

        StringBuilder sb = new StringBuilder();
        File licenseFile;
        FileReader reader;
        BufferedReader br;

        String request;
        String licence;
        if (files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                try {
                    licenseFile = files[i];
                    sb.append("Loading: " + licenseFile.getName() + ":\n");
                    reader = new FileReader(licenseFile);
                    br = new BufferedReader(reader);
                    request = br.readLine();
                    licence = br.readLine();
                    if (internalvalidate(request, licence, sb)) {
                        System.out.println(sb.toString());
                        return true;
                    }
                } catch (Exception ex) {
                    sb.append(ex.getMessage());
                }
            }
            System.out.println(sb.toString());
        }

        try {
            System.out.println("Please enter your name, or company name:");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String username = in.readLine();
            String expiration;
            do {
                System.out.println("Please enter your requested Expiration date (dd/MM/yyyy):");
                expiration = in.readLine();
            }
            while (!validateDate(expiration));
            String data = getMacAddress();
            request = username + delimitter + expiration + delimitter + data;
            System.out.println("Please send the following request to " + EntrepriseConstants.EMAIL + ", to receive a valid license file\n" + request);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }


    private static boolean internalvalidate(String request, String licence, StringBuilder stringBuilder) {
        try {
            Base64.Decoder decoder = Base64.getDecoder();

            //First decrypt with RSA
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(decoder.decode(publicKeyRSA));
            KeyFactory keyFact = KeyFactory.getInstance("RSA");
            PublicKey _publicKey = keyFact.generatePublic(x509KeySpec);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, _publicKey);
            String decrypted = new String(cipher.doFinal(decoder.decode(request)));

            String[] splitted = decrypted.split(delimitter);
            String user = splitted[1];
            String date = splitted[2];
            String mac = splitted[3];

            //validate date:
            if (!validateDate(date)) {
                stringBuilder.append("Licence has expired!");
                return false;
            }

            //validate Mac Address
            String newmac = getMacAddress();
            if (!mac.equals(newmac)) {
                stringBuilder.append("Your hardware has changed, please contact us again to change your licence! Request: " + newmac);
                return false;
            }

            //Most important: validate the license itself
            x509KeySpec = new X509EncodedKeySpec(decoder.decode(publicKey));
            keyFact = KeyFactory.getInstance("EC");
            _publicKey = keyFact.generatePublic(x509KeySpec);

            byte[] bytesRequest = request.getBytes("UTF-8");
            byte[] bytesLicence = decoder.decode(licence);


            Signature signature;
            signature = Signature.getInstance("SHA512withECDSA", "SunEC");
            signature.initVerify(_publicKey);
            signature.update(bytesRequest);
            boolean result = signature.verify(bytesLicence);
            if (!result) {
                stringBuilder.append("The licence is invalid!");
            } else {
                stringBuilder.append(EntrepriseConstants.PRODUCT_NAME + " License for: " + user + ", your licence is activated and valid till " + date + "!");
            }
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }


}
