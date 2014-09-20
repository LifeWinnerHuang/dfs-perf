package pasalab.dfs.perf.benchmark.simpleWrite;

import java.io.IOException;
import java.util.List;

import pasalab.dfs.perf.basic.PerfThread;
import pasalab.dfs.perf.basic.TaskConfiguration;
import pasalab.dfs.perf.benchmark.ListGenerator;
import pasalab.dfs.perf.benchmark.Operators;
import pasalab.dfs.perf.conf.PerfConf;
import pasalab.dfs.perf.fs.PerfFileSystem;

public class SimpleWriteThread extends PerfThread {
  private int mBlockSize;
  private int mBufferSize;
  private long mFileLength;
  private PerfFileSystem mFileSystem;
  private List<String> mWriteFiles;
  private String mWriteType;

  private boolean mSuccess;
  private double mThroughput; // in MB/s

  public boolean getSuccess() {
    return mSuccess;
  }

  public double getThroughput() {
    return mThroughput;
  }

  @Override
  public void run() {
    long timeMs = System.currentTimeMillis();
    long writeBytes = 0;
    mSuccess = true;
    for (String fileName : mWriteFiles) {
      try {
        Operators.writeSingleFile(mFileSystem, fileName, mFileLength, mBlockSize, mBufferSize,
            mWriteType);
        writeBytes += mFileLength;
      } catch (IOException e) {
        LOG.error("Failed to write file " + fileName, e);
        mSuccess = false;
        break;
      }
    }
    timeMs = System.currentTimeMillis() - timeMs;
    mThroughput = (writeBytes / 1024.0 / 1024.0) / (timeMs / 1000.0);
  }

  @Override
  public boolean setupThread(TaskConfiguration taskConf) {
    mBufferSize = taskConf.getIntProperty("buffer.size.bytes");
    mBlockSize = taskConf.getIntProperty("block.size.bytes");
    mFileLength = taskConf.getLongProperty("file.length.bytes");
    mWriteType = taskConf.getProperty("write.type");
    try {
      mFileSystem = Operators.connect((PerfConf.get().DFS_ADDRESS));
      String writeDir = taskConf.getProperty("write.dir");
      int filesNum = taskConf.getIntProperty("files.per.thread");
      mWriteFiles = ListGenerator.generateWriteFiles(mId, filesNum, writeDir);
    } catch (IOException e) {
      LOG.error("Failed to setup thread, task " + mTaskId + " - thread " + mId, e);
      return false;
    }
    mSuccess = false;
    mThroughput = 0;
    return true;
  }

  @Override
  public boolean cleanupThread(TaskConfiguration taskConf) {
    try {
      Operators.close(mFileSystem);
    } catch (IOException e) {
      LOG.warn("Error when close file system, task " + mTaskId + " - thread " + mId, e);
    }
    return true;
  }

}
