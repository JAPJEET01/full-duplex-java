import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FullDuplexAudio {
    public static void main(String[] args) throws LineUnavailableException, IOException {
        // Create a pool of sender and receiver threads
        ExecutorService executor = Executors.newFixedThreadPool(10); // You can adjust the pool size as needed

        // Define the common AudioFormat
        AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);

        // You can add more user addresses as needed
        String[] userAddresses = {"192.168.126.208", "192.168.126.137", "192.168.126.155"};
        int basePort = 12345;

        for (int i = 0; i < userAddresses.length; i++) {
            final String userAddress = userAddresses[i];
            final int senderPort = basePort + i;
            final int receiverPort = basePort + 1000 + i; // Offset receiver port to avoid conflicts

            executor.submit(() -> {
                try {
                    sendAudio(format, userAddress, senderPort);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            executor.submit(() -> {
                try {
                    receiveAudio(format, receiverPort);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void sendAudio(AudioFormat format, String userAddress, int senderPort) throws LineUnavailableException, IOException {
        TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
        microphone.open(format);
        microphone.start();

        InetAddress receiverAddress = InetAddress.getByName(userAddress);

        try (DatagramSocket socket = new DatagramSocket()) {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            System.out.println("Sender for " + userAddress + " is running...");
            while (true) {
                int count = microphone.read(buffer, 0, buffer.length);
                DatagramPacket packet = new DatagramPacket(buffer, count, receiverAddress, senderPort);
                socket.send(packet);
            }
        }
    }

    private static void receiveAudio(AudioFormat format, int receiverPort) throws LineUnavailableException, IOException {
        SourceDataLine speaker = AudioSystem.getSourceDataLine(format);
        speaker.open(format);
        speaker.start();

        try (DatagramSocket socket = new DatagramSocket(receiverPort)) {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            System.out.println("Receiver on port " + receiverPort + " is running...");
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                speaker.write(packet.getData(), 0, packet.getLength());
            }
        }
    }
}
