package rice.p2p.glacier;

import java.util.*;
import rice.p2p.commonapi.*;

/**
 * DESCRIBE THE CLASS
 *
 * @version $Id$
 * @author ahae
 */
public class FragmentKeyRange implements IdRange {

  /**
   * The actual IdRange
   */
  protected IdRange range;

  /**
   * Constructor
   *
   * @param ringId DESCRIBE THE PARAMETER
   * @param range DESCRIBE THE PARAMETER
   */
  protected FragmentKeyRange(IdRange range) {
    this.range = range;
  }

  /**
   * get counterclockwise edge of range
   *
   * @return the id at the counterclockwise edge of the range (inclusive)
   */
  public Id getCCWId() {
    return new FragmentKey(new VersionKey(range.getCCWId(), 0L), 0);
  }

  /**
   * get clockwise edge of range
   *
   * @return the id at the clockwise edge of the range (exclusive)
   */
  public Id getCWId() {
    return new FragmentKey(new VersionKey(range.getCWId(), 0L), 0);
  }

  /**
   * get the complement of this range
   *
   * @return This range's complement
   */
  public IdRange getComplementRange() {
    throw new RuntimeException("FragmentKeyRange.getComplementRange() is not supported!");
  }

  /**
   * returns whether or not this range is empty
   *
   * @return Whether or not this range is empty
   */
  public boolean isEmpty() {
    return range.isEmpty();
  }

  /**
   * test if a given key lies within this range
   *
   * @param key the key
   * @return true if the key lies within this range, false otherwise
   */
  public boolean containsId(Id key) {
    return range.containsId(((FragmentKey)key).key.id);
  }

  /**
   * merges the given range with this range
   *
   * @param merge DESCRIBE THE PARAMETER
   * @return The merge
   */
  public IdRange mergeRange(IdRange merge) {
    throw new RuntimeException("FragmentKeyRange.mergeRange() is not supported!");
  }

  /**
   * diffs the given range with this range
   *
   * @param diff DESCRIBE THE PARAMETER
   * @return The merge
   */
  public IdRange diffRange(IdRange diff) {
    throw new RuntimeException("FragmentKeyRange.diffRange() is not supported!");
  }

  /**
   * intersects the given range with this range
   *
   * @param intersect DESCRIBE THE PARAMETER
   * @return The merge
   */
  public IdRange intersectRange(IdRange intersect) {
    throw new RuntimeException("FragmentKeyRange.intersectRange() is not supported!");
  }

  /**
   * Determines equality
   *
   * @param o DESCRIBE THE PARAMETER
   * @return Equals
   */
  public boolean equals(Object o) {
    throw new RuntimeException("FragmentKeyRange.equals() is not supported!");
  }

  /**
   * Returns the hashCode
   *
   * @return hashCode
   */
  public int hashCode() {
    throw new RuntimeException("FragmentKeyRange.hashCode() is not supported!");
  }

  /**
   * Prints out the string
   *
   * @return A string
   */
  public String toString() {
    throw new RuntimeException("FragmentKeyRange.toString() is not supported!");
  }
}


