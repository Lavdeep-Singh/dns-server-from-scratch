public class Utils {
    public static String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, bytes.length);
    }

    public static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
            if ((i + 1) % 16 == 0) sb.append("\n");
        }
        return sb.toString();
    }

    public static String binaryToAscii(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            if (b >= 32 && b < 127) { // Printable ASCII range
                result.append((char)b);
            } else {
                result.append('.');
            }
        }
        return result.toString();
    }
    

}

