/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hwpf.model;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.poi.hwpf.model.io.HWPFFileSystem;
import org.apache.poi.hwpf.model.io.HWPFOutputStream;
import org.apache.poi.hwpf.sprm.SprmBuffer;
import org.apache.poi.poifs.common.POIFSConstants;
import org.apache.poi.util.LittleEndian;

/**
 * This class holds all of the character formatting properties.
 *
 * @author Ryan Ackley
 */
public class CHPBinTable
{

    private static final class CHPXStartComparator implements Comparator<CHPX>
    {
        static CHPXStartComparator instance = new CHPXStartComparator();

        public int compare( CHPX o1, CHPX o2 )
        {
            int thisVal = o1.getStart();
            int anotherVal = o2.getStart();
            return ( thisVal < anotherVal ? -1 : ( thisVal == anotherVal ? 0
                    : 1 ) );
        }
    }

/** List of character properties.*/
  protected ArrayList<CHPX> _textRuns = new ArrayList<CHPX>();

  /** So we can know if things are unicode or not */
  private TextPieceTable tpt;

  public CHPBinTable()
  {
  }

  /**
   * Constructor used to read a binTable in from a Word document.
   *
   * @param documentStream
   * @param tableStream
   * @param offset
   * @param size
   * @param fcMin
   */
    public CHPBinTable( byte[] documentStream, byte[] tableStream, int offset,
            int size, int fcMin, TextPieceTable tpt )
    {
        /*
         * Page 35:
         * 
         * "Associated with each interval is a BTE. A BTE holds a four-byte PN
         * (page number) which identifies the FKP page in the file which
         * contains the formatting information for that interval. A CHPX FKP
         * further partitions an interval into runs of exception text."
         */
        PlexOfCps bte = new PlexOfCps( tableStream, offset, size, 4 );
        this.tpt = tpt;

    int length = bte.length();
    for (int x = 0; x < length; x++)
    {
      GenericPropertyNode node = bte.getProperty(x);

      int pageNum = LittleEndian.getInt(node.getBytes());
      int pageOffset = POIFSConstants.SMALLER_BIG_BLOCK_SIZE * pageNum;

      CHPFormattedDiskPage cfkp = new CHPFormattedDiskPage(documentStream,
        pageOffset, fcMin, tpt);

      int fkpSize = cfkp.size();

      for (int y = 0; y < fkpSize; y++)
      {
        final CHPX chpx = cfkp.getCHPX(y);
        if (chpx != null)
            _textRuns.add(chpx);
      }
    }
        Collections.sort( _textRuns, CHPXStartComparator.instance );
    }

  public void adjustForDelete(int listIndex, int offset, int length)
  {
    int size = _textRuns.size();
    int endMark = offset + length;
    int endIndex = listIndex;

    CHPX chpx = _textRuns.get(endIndex);
    while (chpx.getEnd() < endMark)
    {
      chpx = _textRuns.get(++endIndex);
    }
    if (listIndex == endIndex)
    {
      chpx = _textRuns.get(endIndex);
      chpx.setEnd((chpx.getEnd() - endMark) + offset);
    }
    else
    {
      chpx = _textRuns.get(listIndex);
      chpx.setEnd(offset);
      for (int x = listIndex + 1; x < endIndex; x++)
      {
        chpx = _textRuns.get(x);
        chpx.setStart(offset);
        chpx.setEnd(offset);
      }
      chpx = _textRuns.get(endIndex);
      chpx.setEnd((chpx.getEnd() - endMark) + offset);
    }

    for (int x = endIndex + 1; x < size; x++)
    {
      chpx = _textRuns.get(x);
      chpx.setStart(chpx.getStart() - length);
      chpx.setEnd(chpx.getEnd() - length);
    }
  }

  public void insert(int listIndex, int cpStart, SprmBuffer buf)
  {

    CHPX insertChpx = new CHPX(0, 0, tpt,buf);

    // Ensure character offsets are really characters
    insertChpx.setStart(cpStart);
    insertChpx.setEnd(cpStart);

    if (listIndex == _textRuns.size())
    {
      _textRuns.add(insertChpx);
    }
    else
    {
      CHPX chpx = _textRuns.get(listIndex);
      if (chpx.getStart() < cpStart)
      {
    	// Copy the properties of the one before to afterwards
    	// Will go:
    	//  Original, until insert at point
    	//  New one
    	//  Clone of original, on to the old end
        CHPX clone = new CHPX(0, 0, tpt,chpx.getSprmBuf());
        // Again ensure contains character based offsets no matter what
        clone.setStart(cpStart);
        clone.setEnd(chpx.getEnd());

        chpx.setEnd(cpStart);

        _textRuns.add(listIndex + 1, insertChpx);
        _textRuns.add(listIndex + 2, clone);
      }
      else
      {
        _textRuns.add(listIndex, insertChpx);
      }
    }
  }

  public void adjustForInsert(int listIndex, int length)
  {
    int size = _textRuns.size();
    CHPX chpx = _textRuns.get(listIndex);
    chpx.setEnd(chpx.getEnd() + length);

    for (int x = listIndex + 1; x < size; x++)
    {
      chpx = _textRuns.get(x);
      chpx.setStart(chpx.getStart() + length);
      chpx.setEnd(chpx.getEnd() + length);
    }
  }

  public List<CHPX> getTextRuns()
  {
    return _textRuns;
  }

  public void writeTo(HWPFFileSystem sys, int fcMin)
    throws IOException
  {

    HWPFOutputStream docStream = sys.getStream("WordDocument");
    OutputStream tableStream = sys.getStream("1Table");

        /*
         * Page 35:
         * 
         * "Associated with each interval is a BTE. A BTE holds a four-byte PN
         * (page number) which identifies the FKP page in the file which
         * contains the formatting information for that interval. A CHPX FKP
         * further partitions an interval into runs of exception text."
         */
        PlexOfCps bte = new PlexOfCps( 4 );

    // each FKP must start on a 512 byte page.
    int docOffset = docStream.getOffset();
    int mod = docOffset % POIFSConstants.SMALLER_BIG_BLOCK_SIZE;
    if (mod != 0)
    {
      byte[] padding = new byte[POIFSConstants.SMALLER_BIG_BLOCK_SIZE - mod];
      docStream.write(padding);
    }

    // get the page number for the first fkp
    docOffset = docStream.getOffset();
    int pageNum = docOffset/POIFSConstants.SMALLER_BIG_BLOCK_SIZE;

    // get the ending fc
    PropertyNode lastRun = (PropertyNode)_textRuns.get(_textRuns.size() - 1); 
    int endingFc = lastRun.getEnd();
    endingFc += fcMin;


    ArrayList<CHPX> overflow = _textRuns;
    do
    {
      PropertyNode startingProp = (PropertyNode)overflow.get(0);
      int start = startingProp.getStart() + fcMin;

      CHPFormattedDiskPage cfkp = new CHPFormattedDiskPage();
      cfkp.fill(overflow);

            byte[] bufFkp = cfkp.toByteArray( tpt, fcMin );
      docStream.write(bufFkp);
      overflow = cfkp.getOverflow();

      int end = endingFc;
      if (overflow != null)
      {
        end = ((PropertyNode)overflow.get(0)).getStart() + fcMin;
      }

      byte[] intHolder = new byte[4];
      LittleEndian.putInt(intHolder, pageNum++);
      bte.addProperty(new GenericPropertyNode(start, end, intHolder));

    }
    while (overflow != null);
    tableStream.write(bte.toByteArray());
  }
}
