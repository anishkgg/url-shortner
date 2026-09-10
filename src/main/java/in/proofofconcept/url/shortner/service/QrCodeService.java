package in.proofofconcept.url.shortner.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class QrCodeService {

    /**
     * Generates a QR Code and returns the raw PNG byte array.
     */
    public byte[] generateQrCodeBytes(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Could not generate QR code", e);
        }
    }

    /**
     * Generates a QR Code and returns the Base64-encoded string.
     */
    public String generateQrCodeBase64(String text, int width, int height) {
        byte[] pngData = generateQrCodeBytes(text, width, height);
        return Base64.getEncoder().encodeToString(pngData);
    }
}
