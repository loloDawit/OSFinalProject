import java.util.Vector;


public class FileTable {
	  private Vector<FileTableEntry> table;         
	  private Directory dir;        

	  public FileTable( Directory directory ) {
	    table = new Vector<FileTableEntry>();      
	    dir = directory; 

	  }                            
	  
	  //Allocates an entry for the file 
	  //Completed::
	  public synchronized FileTableEntry falloc( String filename, String mode ) {

		  short iNumber = -1;
		  Inode inode = null;
		  
		  while (true){
      		  // allocate/retrieve and register the corresponding inode using dir
             iNumber = dir.namei( filename );

			  if(iNumber < 0){						
				  if(mode.charAt(0) == 'r')//No file to be read
					  return null;
				  //Then the mode is write/append, create a file
				  inode = new Inode();
				  iNumber = dir.ialloc(filename);	
			  }
			  else
				  inode = new Inode(iNumber);//otherwise file exist, append to it

			  if(inode.flag < 0) //The file is neither used/unused
                   return null;

			  if((mode.charAt(0) == 'r')){		
				  if(inode.flag != 3){			
					  inode.flag = 2;				
					  break;					
				  }
			  }
			  else{				  
				  if(inode.flag < 2 ){			
					  inode.flag = 3;
					  break;					
				  }
			  }
				  
		  	}
		  
		  // allocate a new file (structure) table entry for this file name
			  FileTableEntry entry = new FileTableEntry(inode,iNumber,mode);
			  table.add(entry);					//add newEntry to table
	     
	      // increment this inode's count
			  inode.count++;						
	      
	      // immediately write back this inode to the disk
			  inode.toDisk(iNumber);					
		  
		   // return a reference to this file (structure) table entry
			  return entry;
	  }

	  /*Completed::
	    receives a file table entry reference
	    Erase the TableEntry and make sure the Inode is constituent that it is not in use
	  */
	  public synchronized boolean ffree( FileTableEntry e ) {
		  int entryIndex = -1; 
		  for(int i = 0; i <table.size(); i++){
			  if(table.elementAt(i).equals(e))
			  		entryIndex = i;
		  }
		  if (entryIndex < 0)						
			  return false;					
           
           //Determine if the node is being used  
          if (e.inode.count - 1 == 0)
                e.inode.flag = 0;

	       //free this file table entry.
		   e.inode.count--;
      	
      	   //save the corresponding inode to the disk
		   e.inode.toDisk(e.iNumber);

      	   //return true if this file table entry found in my table
		   return table.remove(table.elementAt(entryIndex));  
	  }


	  //Completed::
	  public synchronized boolean fempty() {
	    return table.isEmpty();  
      }   
}