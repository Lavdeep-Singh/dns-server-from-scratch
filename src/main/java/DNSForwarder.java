import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class DNSForwarder {
    private final String resolverAddress;
    private final int resolverPort;

    public DNSForwarder(String resolver) {
        String[] parts = resolver.split(":");
        this.resolverAddress = parts[0];
        this.resolverPort = Integer.parseInt(parts[1]);
    }

    public byte[] forwardQuery(byte[] query) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(5000);

            SocketAddress resolver = new InetSocketAddress(resolverAddress, resolverPort);
            DatagramPacket forwardPacket = new DatagramPacket(query, query.length, resolver);
            socket.send(forwardPacket);
            System.out.println("Query forwarded to: " + resolverAddress + ":" + resolverPort);
            System.out.println("Forwarded query ASCII is " + Utils.binaryToAscii(forwardPacket.getData()));

            byte[] responseBuffer = new byte[512];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);

            System.out.println("Resolver response hex dump: " + Utils.bytesToHex(responseBuffer, responsePacket.getLength()));
            System.out.println("Resolver response ASCII is " + Utils.binaryToAscii(responsePacket.getData()));

            byte[] actualResponse = new byte[responsePacket.getLength()];
            System.arraycopy(responseBuffer, 0, actualResponse, 0, responsePacket.getLength());
            return actualResponse;
        } catch (IOException e) {
            System.out.println("Error in forwarding: " + e.getMessage());
            return null;
        }
    }
}
