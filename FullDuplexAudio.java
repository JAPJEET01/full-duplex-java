import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FullDuplexAudio{
    public static void main(String[] args) throws LineUnavailableException, IOException {
        AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);

        // Set up the sender thread
        ExecutorService senderExecutor = Executors.newSingleThreadExecutor();
        senderExecutor.submit(() -> {
            try {
                sendAudio(format);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Set up the receiver thread
        ExecutorService receiverExecutor = Executors.newSingleThreadExecutor();
        receiverExecutor.submit(() -> {
            try {
                receiveAudio(format);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void sendAudio(AudioFormat format) throws LineUnavailableException, IOException {
        TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
        microphone.open(format);
        microphone.start();

        InetAddress receiverAddress = InetAddress.getByName("192.168.29.157");
        int receiverPort = 12345;

        try (DatagramSocket socket = new DatagramSocket()) {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            System.out.println("Sender is running...");
            while (true) {
                int count = microphone.read(buffer, 0, buffer.length);
                DatagramPacket packet = new DatagramPacket(buffer, count, receiverAddress, receiverPort);
                socket.send(packet);
            }
        }
    }

    private static void receiveAudio(AudioFormat format) throws LineUnavailableException, IOException {
        SourceDataLine speaker = AudioSystem.getSourceDataLine(format);
        speaker.open(format);
        speaker.start();

        int port = 12345;

        try (DatagramSocket socket = new DatagramSocket(port)) {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            System.out.println("Receiver is running...");
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                speaker.write(packet.getData(), 0, packet.getLength());
            }
        }
    }
}
