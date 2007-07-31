package org.mpisws.p2p.transport.peerreview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.mpisws.p2p.transport.util.FileInputBuffer;
import org.mpisws.p2p.transport.util.FileOutputBuffer;

import rice.environment.logging.Logger;
import rice.p2p.commonapi.rawserialization.OutputBuffer;

/**
 * The following class implements PeerReview's log. A log entry consists of
 * a sequence number, a type, and a string of bytes. On disk, the log is
 * stored as two files: An index file and a data file.
 * 
 * @author Jeff Hoye
 * @author Andreas Haeberlen
 */
public class SecureHistoryImpl implements SecureHistory {

  Logger logger;
  
  boolean pointerAtEnd;
  IndexEntry topEntry;
  long baseSeq;
  long nextSeq;
  int numEntries;
  File indexFile;
  File dataFile;
  boolean readOnly;
  
  FileOutputBuffer indexFileWriter;
  FileOutputBuffer dataFileWriter;
  
  FileInputBuffer indexFileReader;
  FileInputBuffer dataFileReader;
  
  public SecureHistoryImpl(String baseFileName, boolean readOnly, Logger logger) throws FileNotFoundException {
    this.logger = logger;
    this.readOnly = readOnly;
    indexFile = new File(baseFileName+".index");      
    dataFile = new File(baseFileName+".data");    
    
    if (readOnly) {
      if (!indexFile.exists()) throw new IllegalArgumentException("File "+indexFile+" doesn't exist.");
      if (!dataFile.exists()) throw new IllegalArgumentException("File "+dataFile+" doesn't exist.");
    }
    initializeFileBuffers();
  }

  private void initializeFileBuffers() throws FileNotFoundException {    
    indexFileReader = new FileInputBuffer(indexFile, logger);
    dataFileReader = new FileInputBuffer(dataFile, logger);
    
    if (!readOnly) {
      indexFileWriter = new FileOutputBuffer(indexFile);
      dataFileWriter = new FileOutputBuffer(dataFile);
    }
  }
  
  public void reset(long baseSeq, Hash nodeHash) throws IOException {
    if (indexFile.exists()) indexFile.delete();
    if (dataFile.exists()) dataFile.delete();
    initializeFileBuffers();
    /* Write the initial record to the index file. The data file remains empty. */
    
    IndexEntry entry = new IndexEntry(baseSeq, (short)0, (short)0, (short)-1, null, nodeHash);
    
//    fwrite(&entry, sizeof(entry), 1, indexFile);
    writeEntry(entry, indexFileWriter);
  }

  private void writeEntry(IndexEntry entry, OutputBuffer fileWriter) throws IOException {
    entry.serialize(fileWriter);
  }
  
  public long getBaseSeq() {
    return baseSeq;
  }

  public long getLastSeq() {
    return topEntry.seq;
  }

  public int getNumEntries() {
    return numEntries;
  }

  
  class IndexEntry {
    long seq;
    short fileIndex;
    short sizeInFile;
    short type;
    Hash contentHash;
    Hash nodeHash;
    
    public IndexEntry(long seq, short type, short index, short size, Hash contentHash, Hash nodeHash) {
      this.seq = seq;
      this.type = type;
      this.fileIndex = index;
      this.sizeInFile = size;
      this.contentHash = contentHash;
      this.nodeHash = nodeHash;
    }

    public void serialize(OutputBuffer buf) throws IOException {
      buf.writeLong(seq);
      buf.writeInt(fileIndex);
      buf.writeInt(sizeInFile);
      buf.writeInt(type);
      contentHash.serialize(buf);
      nodeHash.serialize(buf);
    }
  }
}
