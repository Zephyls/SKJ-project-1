import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class DAS {
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java DAS <port> <number>");
            System.exit(1);
        }

        int port;
        int number;
        try {
            port = Integer.parseInt(args[0]);
            number = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid parameters. <port> and <number> must be integers.");
            System.exit(1);
            return;
        }

        try {
            DatagramSocket socket = new DatagramSocket(port);
            System.out.println("Running in master mode on port " + port);
            runMasterMode(socket, port, number);
        } catch (SocketException e) {
            System.out.println("Port " + port + " is already in use. Running in slave mode.");
            runSlaveMode(port, number);
        }
    }

    private static void runMasterMode(DatagramSocket socket, int port, int initialNumber) {
        List<Integer> numbers = new ArrayList<>();
        numbers.add(initialNumber);

        byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());

                int receivedNumber = Integer.parseInt(message.trim());
                if (receivedNumber == -1) {
                    System.out.println("Received -1. Terminating master mode.");
                    broadcastMessage(socket, port, "-1");
                    socket.close();
                    break;
                } else if (receivedNumber == 0) {
                    int average = (int) Math.floor(numbers.stream().filter(n -> n != 0).mapToInt(Integer::intValue).average().orElse(0));
                    System.out.println("Computed average: " + average);
                    broadcastMessage(socket, port, String.valueOf(average));
                } else {
                    System.out.println("Received number: " + receivedNumber);
                    numbers.add(receivedNumber);
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error processing received packet: " + e.getMessage());
            }
        }
    }

    private static void runSlaveMode(int port, int number) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getLocalHost();
            String message = String.valueOf(number);
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, port);
            socket.send(packet);
            System.out.println("Sent number " + number + " to master on port " + port);
        } catch (IOException e) {
            System.err.println("Error in slave mode: " + e.getMessage());
        }
    }

    private static void broadcastMessage(DatagramSocket socket, int port, String message) {
        try {
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), broadcastAddress, port);
            socket.send(packet);
            System.out.println("Broadcasted message: " + message);
        } catch (IOException e) {
            System.err.println("Error broadcasting message: " + e.getMessage());
        }
    }
}
