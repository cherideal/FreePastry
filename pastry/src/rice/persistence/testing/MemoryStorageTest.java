
package rice.persistence.testing;

/*
 * @(#) MemoryStorageTest.java
 *
 * @author Ansley Post
 * @author Alan Mislove
 * 
 * @version $Id$
 */
import java.io.*;
import java.util.*;

import rice.*;
import rice.environment.Environment;
import rice.p2p.commonapi.*;
import rice.pastry.commonapi.*;
import rice.persistence.*;

/**
 * This class is a class which tests the Storage class
 * in the rice.persistence package.
 */
public class MemoryStorageTest extends Test {

  private IdFactory FACTORY;// = new PastryIdFactory();
  
  protected Storage storage;

  protected boolean store;
  private Id[] data;
  private Integer[] metadata;

  /**
   * Builds a MemoryMemoryStorageTest
   */
  public MemoryStorageTest(boolean store, Environment env) {
    super(env);
    FACTORY = new PastryIdFactory(env);
    storage = new MemoryStorage(FACTORY);
    data  = new Id[500];
    metadata = new Integer[500];
    int[] x = new int[5];
    for (int i = 0; i < 500; i ++){
        x[3] = i;
      	data[i] = FACTORY.buildId(x);
        metadata[i] = new Integer(i);
    }
    this.store = store;
  }

  public void setUp(final Continuation c) {
    final Continuation put4 = new Continuation() {
      public void receiveResult(Object o) {
        if (o.equals(new Boolean(true))) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE, "Fourth object was not inserted.");
          return;
        }

        sectionEnd();
        c.receiveResult(new Boolean(true));
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    final Continuation put3 = new Continuation() {
      public void receiveResult(Object o) {
        if (o.equals(new Boolean(true))) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE, "Third object was not inserted.");
          return;
        }

        stepStart("Storing Fourth Object");
        storage.store(data[4], metadata[4], "Fourth Object", put4);
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    final Continuation put2 = new Continuation() {
      public void receiveResult(Object o) {
        if (o.equals(new Boolean(true))) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE, "Second object was not inserted.");
          return;
        }

        stepStart("Storing Third Object");
        storage.store(data[3], metadata[3], "Third Object", put3);
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    Continuation put1 = new Continuation() {
      public void receiveResult(Object o) {
        if (o.equals(new Boolean(true))) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE, "First object was not inserted.");
          return;
        }

        stepStart("Storing Second Object");
        storage.store(data[2], metadata[2], "Second Object", put2);
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    sectionStart("Storing Objects");
    
    stepStart("Storing First Object");
    storage.store(data[1], metadata[1], "First Object", put1);
  }

  public void testRetreival(final Continuation c) {
    final Continuation get5 = new Continuation() {
      public void receiveResult(Object o) {
        if (o == null) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE, "Fifth object was returned (should not be present).");
          return;
        }

        sectionEnd();
        c.receiveResult(new Boolean(true));
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    final Continuation get4 = new Continuation() {
      public void receiveResult(Object o) {
        if (o == null) {
          stepDone(FAILURE, "Returned object was null.");
          return;
        }
        
        if (o.equals("Fourth Object")) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE, "Returned object was not correct: " + o);
          return;
        }

        stepStart("Attempting Fifth Object");
        storage.getObject(data[5], get5);
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };
    
    final Continuation get3 = new Continuation() {
      public void receiveResult(Object o) {
        if (o == null) {
          stepDone(FAILURE, "Returned object was null.");
          return;
        }
        
        if (o.equals("Third Object")) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE, "Returned object was not correct: " + o);
          return;
        }

        stepStart("Retrieving Fourth Object");
        storage.getObject(data[4], get4);
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    final Continuation get2 = new Continuation() {
      public void receiveResult(Object o) {
        if (o == null) {
          stepDone(FAILURE, "Returned object was null.");
          return;
        }
        
        if (o.equals("Second Object")) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE, "Returned object was not correct: " + o);
          return;
        }

        stepStart("Retrieving Third Object");
        storage.getObject(data[3], get3);
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    final Continuation get1 = new Continuation() {
      public void receiveResult(Object o) {
        if (o == null) {
          stepDone(FAILURE, "Returned object was null.");
          return;
        }
        
        if (o.equals("First Object")) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE, "Returned object was not correct: " + o);
          return;
        }

        stepStart("Retrieving Second Object");
        storage.getObject(data[2], get2);
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };
    
    Continuation get0 = new Continuation() {
      public void receiveResult(Object o) {
        if (o.equals(new Boolean(true))) {
          sectionStart("Retrieving Objects");

          stepStart("Retrieving First Object");
          storage.getObject(data[1], get1);
        } else {
          stepException(new RuntimeException("SetUp did not complete correctly."));
        }
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    if (store) {
      setUp(get0);
    } else {
      get1.receiveResult("First Object");
    }
  }

  public void testExists(final Continuation c) {
    testRetreival(new Continuation() {
      public void receiveResult(Object o) {
        if (o.equals(new Boolean(true))) {
          sectionStart("Checking for Objects");
          stepStart("Checking for First Object");
          if (storage.exists(data[1])) stepDone(SUCCESS); else stepDone(FAILURE);
          stepStart("Checking for Second Object");
          if (storage.exists(data[2])) stepDone(SUCCESS); else stepDone(FAILURE);
          stepStart("Checking for Third Object");
          if (storage.exists(data[3])) stepDone(SUCCESS); else stepDone(FAILURE);
          stepStart("Checking for Fourth Object");
          if (storage.exists(data[4])) stepDone(SUCCESS); else stepDone(FAILURE);
          stepStart("Checking for Fifth Object");
          if (! storage.exists(data[5])) stepDone(SUCCESS); else stepDone(FAILURE);
          sectionEnd();
          
          sectionStart("Checking for Metadata");
          stepStart("Checking for First Object Metadata");
          if (metadata[1].equals(storage.getMetadata(data[1]))) stepDone(SUCCESS); else stepDone(FAILURE);
          stepStart("Checking for Second Object Metadata");
          if (metadata[2].equals(storage.getMetadata(data[2]))) stepDone(SUCCESS); else stepDone(FAILURE);
          stepStart("Checking for Third Object Metadata");
          if (metadata[3].equals(storage.getMetadata(data[3]))) stepDone(SUCCESS); else stepDone(FAILURE);
          stepStart("Checking for Fourth Object Metadata");
          if (metadata[4].equals(storage.getMetadata(data[4]))) stepDone(SUCCESS); else stepDone(FAILURE);
          stepStart("Checking for Fifth Object Metadata");
          if (! metadata[5].equals(storage.getMetadata(data[5]))) stepDone(SUCCESS); else stepDone(FAILURE);
          sectionEnd();
          
          sectionStart("Modifying Metadata");
          stepStart("Changing Metadata");
          storage.setMetadata(data[4], new Integer(5001), new Continuation() {
            public void receiveResult(Object o) {
              stepDone(SUCCESS);
              
              stepStart("Checking for New Metadata");
              if ((new Integer(5001)).equals(storage.getMetadata(data[4]))) stepDone(SUCCESS); else stepDone(FAILURE);
              sectionEnd();
              
              c.receiveResult(new Boolean(true));
            }
            
            public void receiveException(Exception e) {
              stepException(e);
            }
          });
        } else {
          throw new RuntimeException("SetUp did not complete correctly!");
        }
      }
      
      public void receiveException(Exception e) {
        stepException(e);
      }
    });
  }  

  private void testRemoval(final Continuation c) {
    final Continuation done1 = new Continuation() {
      public void receiveResult(Object o) {
        if (o == null) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE);
        }

        sectionEnd();

        c.receiveResult(new Boolean(true));
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };
    
    
    final Continuation retrieve1 = new Continuation() {
      public void receiveResult(Object o) {
        
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };
    
    final Continuation check1 = new Continuation() {
      public void receiveResult(Object o) {
        if ((! store) || o.equals(new Boolean(true))) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE);
        }

        stepStart("Checking for First Object");
        boolean result = storage.exists(data[1]);
        
        if ((! store) || (! result)) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE);
        }
        
        stepStart("Attempting to Retrieve First Object");
        storage.getObject(data[1], done1);
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    Continuation remove1 = new Continuation() {
      public void receiveResult(Object o) {
        if (o.equals(new Boolean(true))) {
          sectionStart("Testing Removal");

          stepStart("Removing First Object");
          storage.unstore(data[1], check1);
        } else {
          stepException(new RuntimeException("Exists did not complete correctly."));
        }
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    testExists(remove1);
  }

  private void testScan(final Continuation c) {
    final Continuation handleBadScan = new Continuation() {
      public void receiveResult(Object o) {
        stepDone(FAILURE, "Query returned; should have thrown exception");
      }

      public void receiveException(Exception e) {
        stepDone(SUCCESS);

        sectionEnd();

        c.receiveResult(new Boolean(true));
      }
    };

    final Continuation query = new Continuation() {
      public void receiveResult(Object o) {
        if (o.equals(new Boolean(true))) {
          stepDone(SUCCESS);
        } else {
          stepDone(FAILURE);
        }

        stepStart("Requesting Scan from 3 to 6");
        IdSet result = storage.scan(FACTORY.buildIdRange(data[3], data[6]));
        
        if (result.numElements() != 2) {
          stepDone(FAILURE, "Result had " + result.numElements() + " elements, expected 2.");
          return;
        }
        
        
        if (! ((result.isMemberId(data[3])) && (result.isMemberId(data[4])))) {
          stepDone(FAILURE, "Result had incorrect elements " + data[0] + ", " + data[1] + ", expected 3 and 4.");
          return;
        }
        
        stepDone(SUCCESS);
        
        stepStart("Requesting Scan from 8 to 10");
        result = storage.scan(FACTORY.buildIdRange(data[8], data[10]));
        
        if (result.numElements() != 0) {
          stepDone(FAILURE, "Result had " + result.numElements() + " elements, expected 0.");
          return;
        }
        
        stepDone(SUCCESS);
        
        stepStart("Requesting Scan from 'Monkey' to 9");
        handleBadScan.receiveException(new Exception()); 
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    Continuation insertString = new Continuation() {
      public void receiveResult(Object o) {
        if (o.equals(new Boolean(true))) {
          sectionStart("Testing Scan");

          stepStart("Inserting String as Key");
          storage.store(data[11], null, "Monkey", query);
        } else {
          stepException(new RuntimeException("Removal did not complete correctly."));
        }
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    testRemoval(insertString);
  }

  private void testRandomInserts(final Continuation c) {
    
    final int START_NUM = 10;
    final int END_NUM = 98;
    final int SKIP = 2;

    final int NUM_ELEMENTS = 1 + ((END_NUM - START_NUM) / SKIP);
    
    final Continuation checkRandom = new Continuation() {
      public void receiveResult(Object o) {
        stepStart("Checking object deletion");
        int NUM_DELETED = ((Integer) o).intValue();
        int length = storage.scan(FACTORY.buildIdRange(data[13 + START_NUM], data[13 + END_NUM + SKIP])).numElements();
        
        int desired = NUM_ELEMENTS - NUM_DELETED;
        
        if (length == desired) {
          stepDone(SUCCESS);
          
          sectionEnd();
          c.receiveResult(new Boolean(true));
        } else {
          stepDone(FAILURE, "Expected " + desired + " objects after deletes, found " + length + ".");
          return;
        }
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    
    final Continuation removeRandom = new Continuation() {
      
      private int count = START_NUM;
      private int num_deleted = 0;

      public void receiveResult(Object o) {
        if (count == START_NUM) {
          stepStart("Removing random subset of objects");
        }

        if (o.equals(new Boolean(false))) {
          stepDone(FAILURE, "Deletion of " + count + " failed.");
          return;
        }

        if (count == END_NUM) {
          stepDone(SUCCESS);
          checkRandom.receiveResult(new Integer(num_deleted));
          return;
        }

        if (environment.getRandomSource().nextBoolean()) { 
          num_deleted++;
          storage.unstore(data[13 + (count += SKIP)], this);
        } else {
          count += SKIP;
          receiveResult(new Boolean(true));
        }
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    final Continuation checkScan = new Continuation() {

      private int count = START_NUM;
      
      public void receiveResult(Object o) {
          stepStart("Checking scans for all ranges");

        for (int count = START_NUM; count <  END_NUM - SKIP; count+=SKIP) {
          IdSet result = storage.scan(FACTORY.buildIdRange(data[13 + (count += SKIP)], data[13 + END_NUM]));
          
          int i = NUM_ELEMENTS - ((count - START_NUM + SKIP) / SKIP) ;
          if (result.numElements() != i){
            stepDone(FAILURE, "Expected " + i + " found " + result.numElements() + " keys in scan from " + count + " to " + END_NUM + ".");
            return;
          }
        }
        
        stepDone(SUCCESS);
        removeRandom.receiveResult(new Boolean(true));
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };
    
    final Continuation checkExists = new Continuation() {
      public void receiveResult(Object o) {
        stepStart("Checking exists for all 50 objects");
        
        for (int count = START_NUM - SKIP; count < END_NUM; count+=SKIP) {
          boolean b = storage.exists(data[13 + count]);
          
          if (! b) {
            stepDone(FAILURE, "Element " + count + " did exist - should not have.");
            return;
          }
        }
        
        stepDone(SUCCESS);
        checkScan.receiveResult(new Boolean(true));
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };
    
    
    final Continuation insert = new Continuation() {

      private int count = START_NUM;

      public void receiveResult(Object o) {
        if (o.equals(new Boolean(false))) {
          stepDone(FAILURE, "Insertion of " + count + " failed.");
          return;
        }
        
        if (count == START_NUM) {
          sectionStart("Stress Testing");
          stepStart("Inserting 40 objects from 100 to 1000000 bytes");
        }

        if (count > END_NUM) {
          stepDone(SUCCESS);
          checkExists.receiveResult(new Boolean(true));
          return;
        }

        int num = count;
        count += SKIP;
        
        storage.store(data[13 + num], null, new byte[num * num * num], this);
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    testScan(insert);
  }


  private void testErrors() {
    final Continuation validateNullValue = new Continuation() {

      public void receiveResult(Object o) {
        if (o.equals(new Boolean(true))) {
          stepDone(FAILURE, "Null value should return false");
          return;
        }

        stepDone(SUCCESS);

        sectionEnd();        
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    final Continuation insertNullValue = new Continuation() {

      public void receiveResult(Object o) {
        if (o.equals(new Boolean(true))) {
          stepDone(FAILURE, "Null key should return false");
          return;
        }

        stepDone(SUCCESS);

        stepStart("Inserting null value");

        storage.store(data[12], null, null, validateNullValue);
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    final Continuation insertNullKey = new Continuation() {

      public void receiveResult(Object o) {
        if (o.equals(new Boolean(false))) {
          stepDone(FAILURE, "Randon insert tests failed.");
          return;
        }

        sectionStart("Testing Error Cases");
        stepStart("Inserting null key");

        storage.store(null, null, "null key", insertNullValue);
      }

      public void receiveException(Exception e) {
        stepException(e);
      }
    };

    testRandomInserts(insertNullKey);
  }
  
  public void start() {
    testErrors();
	try{	Thread.sleep(20000);}catch(InterruptedException ie){;}
  }

  public static void main(String[] args) throws IOException {
    MemoryStorageTest test = new MemoryStorageTest(true, new Environment());

    test.start();
  }
}
