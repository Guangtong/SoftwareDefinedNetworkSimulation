import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class Log {
	private String filename;
	private BufferedWriter bw = null;
	private FileWriter fw = null;
	private PrintWriter pw = null;
	
	protected void finalize() {
		this.close();
	}
	
	public Log(String filename) throws IOException {
		this.filename = filename;

		try {
			fw = new FileWriter(filename, true); //true: append to end
		} catch (IOException e) {
			System.err.println("Error opening file: " + filename);
			throw e;
		} 
		bw = new BufferedWriter(fw);
		pw = new PrintWriter(bw);
		pw.println(LocalDateTime.now() +  ": LOG file created." );
	}
	
	public void println(String s) {
		System.out.println(s);
		pw.println(LocalDateTime.now() +  ": "+ s);
	}

	public void println() {
		System.out.println();
		pw.println();
	}
	
	public void print(String s) {
		System.out.print(s);
		try {
			bw.write(s);
			bw.flush();
		} catch (IOException e) {
			System.err.println("Error writing to file: " + filename);
		}
	}
	
	public void errPrintln(String s) {
		System.err.println(s);
		pw.println(LocalDateTime.now() +  ": "+ "ERROR! " + s + '\n');
	}
	
	public void close() {
		try {
			if (bw != null)
				bw.close();
			if (fw != null)
				fw.close();
			if (pw != null)
				pw.close();
		} catch (IOException ex) {
			System.err.println("Error closing file: " + filename);
		}
	}	
}
