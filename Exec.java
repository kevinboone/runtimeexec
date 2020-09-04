/** Exec.java is an example showing how to use Runtime.exec() in a 
  * reasonably robust way. 
  *
  * Do with this code as you see fit, at your own risk. 
  */

import java.io.*;

/** StreamEater is a class that reads an InputStream to exhaustion. All the 
  * data read from the stream is appended to a StringBuffer, which the caller
  * can later read. The intended usage pattern is that the stdout and stderr
  * streams from a Process are hooked up to StreamEater threads, then the
  * threads are started. The threads capture data from the streams as the
  * process runs, until the stream is finished or until an exception is 
  * raised. An exception is unlikely to be raised in practice. 
  *
  * Note that the contents of the stream are added to the buffer line-by-line,
  * and end-of-lines are preserved. In practice, we might want to do 
  * some processing, rather than just absorbing data. In any event, we can't
  * practically absorb the data from a process that produces a huge amount
  * of output. */
class StreamEater extends Thread
  {
  BufferedReader br;
  IOException e = null;
  StringBuffer sb = new StringBuffer();

  /** Construct a StreamEater on an InputStream. */
  public StreamEater (InputStream is)
    {
    this.br = new BufferedReader (new InputStreamReader (is));
    }

  /* Execution starts from here when start() is called on this object, and
   * continues until the stream is closed (usually when the process finishes)
   * or until an exception is raised. */
  public void run ()
    {
    try
      {
      String line;
      while ((line = br.readLine()) != null)
        {
	sb.append (line);
	sb.append ("\n");
	}
      }
    catch (IOException e)
      {
      this.e = e;
      }
    finally
      {
      try { br.close(); } catch (Exception e) {};
      }
    }

  /* Get the exception raised while reading the stream, or null if 
   * there was no exception. */
  public IOException getException() { return e; }
  /* Get the buffered containing the stream contents. */
  public StringBuffer getBuffer() { return sb; }
  }

/** Class Exec demonstrates how to use Runtime.exec() with StreamEater threads
  * to absorb the process stdout and stderr. */
public class Exec
  {
  public static void main (String args[])
    {
    try
      {
      // Create the process and gets its stdout and stderr streams
      // Note that we must use an array of arguments here, because "ls -l"
      // is a single argument to "sh", and must not be split
      Process p = Runtime.getRuntime().exec 
        (new String[]{"sh", "-c", "./something.sh 42"});

      // Get the stdout and stderr streams
      InputStream stdout = p.getInputStream();
      InputStream stderr = p.getErrorStream();
      
      // Create and start the StreamEater threads
      StreamEater stdoutEater = new StreamEater (stdout);
      StreamEater stderrEater = new StreamEater (stderr);
      stdoutEater.start();
      stderrEater.start();
      
      // Wait for the process to complete, and print its exit status
      p.waitFor();
      int e = p.exitValue();
      System.out.println ("Exit code = " + e);

      // Process the results from stdout and stderr. In this case, we just 
      //  print the whole contents. In practice, more complex processing
      //  might be required.
      IOException ioe;
      ioe = stdoutEater.getException();
      if (ioe != null) 
        System.out.println ("Could not read stdout: " + ioe.toString());
      else
        System.out.println ("Contents of stdout: " + stdoutEater.getBuffer()); 
      ioe = stderrEater.getException();
      if (ioe != null) 
        System.out.println ("Could not read stderr: " + ioe.toString());
      else
        System.out.println ("Contents of stderr: " + stderrEater.getBuffer()); 
      }
    catch (IOException e)
      {
      // An IOException typically means tha the process could not be
      // executed at all
      e.printStackTrace();
      }
    catch (InterruptedException e)
      {
      // Handle a situation where the waitFor() method is interrupted. This
      // should never happen unless some other application thread can 
      // actually interact with this one.
      e.printStackTrace();
      }
    }

  }

  // Can't call exitValue() without waitFor() -- IllegalThreadStateException
  // exec() will throw an IOException if the command is not executable
  // exec ("mkdir \"Hello World\"") produces two files "Hello and World"
  // It's not a shell -- you can't do redirection, etc. Can't do $HOME
  
  
