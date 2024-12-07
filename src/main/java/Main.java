import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Main {
  public static void main(String[] args) {
    setConfigWithArguments(args); // set's arguments in config map
    // Creates a UDP server socket on port 2053
    try (DatagramSocket serverSocket = new DatagramSocket(2053)) {
      while (true) { // Continuous loop to handle incoming requests
        // Create a buffer to store incoming data (DNS packets are typically <= 512
        // bytes)
        byte[] buf = new byte[512];
        // Create a packet container for the incoming data
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        // Wait for and receive incoming packet
        serverSocket.receive(packet);
        // Log received request
        System.out.println("Received request from client:");
        System.out.println("Request length: " + packet.getLength());
        System.out.println("Request dump: " + Utils.bytesToHex(packet.getData(), packet.getLength()));
        System.out.println("Request ASCII " + Utils.binaryToAscii(packet.getData()));

        // Create DNS header response
        DNSQueryHandler queryHandler = new DNSQueryHandler(packet);
        byte[] response = queryHandler.resolveQuery();

        // Log response before sending
        System.out.println("Sending response to client:");
        System.out.println("Response length: " + response.length);
        System.out.println("Response hex dump: " + Utils.bytesToHex(response, response.length));
        System.out.println("Response ASCII " + Utils.binaryToAscii(response));

        // Create response packet with client's address from original packet
        DatagramPacket packetResponse = new DatagramPacket(
            response,
            response.length,
            packet.getSocketAddress());
        // Send the response back to client
        serverSocket.send(packetResponse);
      }
    } catch (IOException e) {
      System.out.println("DNS server encountered IOException: " + e.getMessage());
    }
  }

  private static void setConfigWithArguments(String[] args) {
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.equalsIgnoreCase("--resolver")) {
        Config.setConfig("resolver", args[++i]);
      }
    }
  }
}

/*
 * Protocol:
 * ServerSocket uses TCP (Transmission Control Protocol)
 * DatagramSocket uses UDP (User Datagram Protocol)
 * Connection handling:
 * ServerSocket creates connection-oriented communication where a dedicated
 * channel is established between client and server
 * DatagramSocket is connectionless - packets are sent independently without
 * establishing a connection first
 * Data reliability:
 * ServerSocket (TCP) guarantees delivery and correct ordering of data
 * DatagramSocket (UDP) provides no guarantees - packets may arrive out of order
 * or get lost
 * Use cases:
 * ServerSocket is ideal for applications requiring reliable data transfer like
 * web servers, file transfers
 * DatagramSocket is better for speed-sensitive applications like DNS, video
 * streaming, gaming where some packet loss is acceptable
 * In this DNS server implementation, DatagramSocket is used because:
 * DNS traditionally uses UDP on port 53 (using 2053 for testing)
 * DNS queries are typically small and self-contained
 * The speed advantage of UDP is more important than guaranteed delivery
 * If a DNS query fails, clients can simply retry
 * The code shows this UDP behavior where individual packets are received and
 * sent without maintaining connection state.
 */

/*
 * DNS Packet Structure and Sizes
 * -----------------------------
 * Maximum UDP DNS packet size: 512 bytes
 * 
 * Header Section (12 bytes fixed):
 * - ID (2 bytes)
 * - Flags (2 bytes)
 * - Question Count (2 bytes)
 * - Answer Count (2 bytes)
 * - Authority Count (2 bytes)
 * - Additional Count (2 bytes)
 * 
 * Question Section (variable size):
 * - QNAME: Domain name in label format (variable)
 * - QTYPE: Query type (2 bytes)
 * - QCLASS: Query class (2 bytes)
 * 
 * Answer Section (variable size):
 * - NAME: Domain name in label/pointer (variable)
 * - TYPE: Record type (2 bytes)
 * - CLASS: Class code (2 bytes)
 * - TTL: Time to live (4 bytes)
 * - RDLENGTH: Length of RDATA (2 bytes)
 * - RDATA: Resource data (variable)
 * 
 * Authority Section (variable size):
 * - Same structure as Answer Section
 * 
 * Additional Section (variable size):
 * - Same structure as Answer Section
 * 
 * Note: While UDP DNS packets are limited to 512 bytes,
 * TCP DNS can handle larger sizes with a 2-byte length prefix
 */
