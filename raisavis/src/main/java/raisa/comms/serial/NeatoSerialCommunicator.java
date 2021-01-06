package raisa.comms.serial;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import raisa.comms.ControlMessage;
import raisa.comms.SensorListener;

public class NeatoSerialCommunicator extends AbstractSerialCommunicator {

    private static final Logger log = LoggerFactory
            .getLogger(NeatoSerialCommunicator.class);

    private Socket neatoSocket;

    private Thread t;
    private boolean listening = false;
    private BufferedReader input;
    private OutputStream output;
    private Deque<byte[]> commands = new ConcurrentLinkedDeque<>();

    @Override
    public boolean connect() {
        try {
            neatoSocket = new Socket("192.168.1.50", 20042);

            output = neatoSocket.getOutputStream();
            input = new BufferedReader(new InputStreamReader(
                    neatoSocket.getInputStream(), StandardCharsets.UTF_8));

            listening = true;

            t = new Thread() {
                public void run() {
                    while (listening) {
                        try {
                            String sb = input.readLine();
                            for (SensorListener sensorListener : sensorListeners) {
                                sensorListener.sampleReceived(sb);
                            }
                            if (commands.peekLast() != null) {
                                output.write(commands.removeLast());
                            } else {
                                output.write("X".getBytes());
                            }
                            output.flush();
                        } catch (Exception ex) {
                            log.warn("Failed to send scan", ex);
                            throw new RuntimeException(ex);
                        }
                    }
                }
            };
            t.start();
            return true;
        } catch (Exception ex) {
            log.error("Failed to connect to Neato", ex);
            listening = false;
        }
        return false;
    }

    @Override
    public void close() {
        if (listening) {
            listening = false;
        }
        if (neatoSocket != null) {
            try {
                Thread.sleep(1000);
                neatoSocket.close();
            } catch (Exception ex) {
                log.warn("Failed to close Neato connection", ex);
            }
        }
        neatoSocket = null;
        output = null;
        input = null;
    }

    @Override
    public void sendPackage(ControlMessage controlMessage) {
        if (!active) {
            return;
        }
        /*
         * if (output != null) { synchronized(output) { try { String msg = "m "
         * + (controlMessage.getLeftSpeed() * 1000) + " " + 
         * (controlMessage.getRightSpeed() * 1000) + " 200"; if
         * (controlMessage.getLeftSpeed() == 0 && controlMessage.getRightSpeed()
         * == 0) { msg = "m 1 1 1"; } log.info("Sending message {}" + msg);
         * output.write(msg); output.flush(); } catch (Exception ex) {
         * log.error("Failed to send control message", ex); } } }
         */
        commands.addFirst(controlMessage.toJson().getBytes(UTF_8));
	}
}
