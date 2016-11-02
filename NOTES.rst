IDS Locking Implementation Notes
================================

This document contains an unsorted collection of notes and
observations made while coding on the implementation of a new locking
mechanism for ids.server.


Implementation
~~~~~~~~~~~~~~

LockManager
-----------

The new LockManager class has been implemented as a Singleton to
manage the locks and to obtain the locks from.

AlreadyLockedException
----------------------

This is the exception to be raised by the lock() method if it was not
able to acquire the lock.  For the moment, the class is nested inside
the LockManager.  A better place may be found later on.  In
particular, for the `Locking in Storage Plugin Option`_, the plugin
must also be able to raise it.  Maybe move it to the package
`org.icatproject.ids.plugin` then?

getServiceStatus
----------------

The getServiceStatus web service call used to return something like::

  {'opsQueue': [{'request': 'WRITE_THEN_ARCHIVE_REQUESTED', 'data': '911105/911235 (ESNF/12100409-ST/1.1-P/test_race_upload_archive)'}], 'lockCount': 1, 'lockedIds': [911235]}

Note the field `lockedIds` which is a list of all datasets ids having
a lock set.  The new implementation now returns::

  {'opsQueue': [{'request': 'ARCHIVE_REQUESTED', 'data': '911105/911238 (ESNF/12100409-ST/1.1-P/test_race_upload_archive)'}], 'lockCount': 1, 'locks': [{'id': 911238, 'count': 2, 'type': 'SHARED'}]}

The field `lockedIds` has been renamed to `locks`.  The entries in
this list now have the three attributes `id`, `type`, `count`, where
id is the dataset id, type is either SHARED or EXCLUSIVE, and count is
the number of locks.  If compatibility with the old behavior is
desired, this can easily be changed.

Unsorted Notes
--------------

 * Locks are always set on whole datasets, even in the case of
   storageUnit datafile.  Note that this was also the case in the old
   implementation.  I also believe that this is good, because the
   setting for the storage unit is a configuration detail that should
   not have an impact on the behavior of the IDS server visible to the
   client.

 * Web service calls that fail because the corresponding data is
   locked will throw a DataNotOnlineException for the moment.  While
   the previous definition of the error condition might not quite
   match this case, from the client's point of view it's the same: it
   is a temporary failure and the client may retry the same request
   later on.  We still may define another more specific error class or
   even redefine DataNotOnlineException.

 * Although the put web service call is actually writing to the
   dataset, it only acquires a SHARED lock.  This is because it does
   not modify any existing data, so it may be run in parallel with
   other read accesses or put calls.


Observations Regarding the Old Code
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Old Locking
-----------

The current locking mechanism is used in IdsBean.  Some operations in
the FiniteStateMachine check for locks.

Places where locking, unlocking, and checking for locks occurs:

 * class IdsBean.SO get an ARCHIVE_AND_DELETE lock as argument to the
   constructor.  The write() method finally unlocks.

 * IdsBean.checkDatafilesPresent() gets an ARCHIVE_AND_DELETE lock as
   argument and unlocks on failure.

 * IdsBean.checkOnlineAndFreeLockOnFailure() gets a lock as argument
   and unlocks on failure.

 * IdsBean.delete() acquires an ARCHIVE lock (only if storage unit
   dataset).  It calls checkOnlineAndFreeLockOnFailure().  It checks
   for a DELETE lock.

 * IdsBean.getData(preparedId, ...) acquires an ARCHIVE_AND_DELETE
   lock.  It calls checkOnlineAndFreeLockOnFailure() and
   checkDatafilesPresent().  It finally passes the lock over to
   IdsBean.SO.

 * IdsBean.getData(sessionId, ...) acquires an ARCHIVE_AND_DELETE
   lock.  It calls checkOnlineAndFreeLockOnFailure() and
   checkDatafilesPresent().  It finally passes the lock over to
   IdsBean.SO.

 * IdsBean.put() acquires an ARCHIVE lock.  It calls
   checkOnlineAndFreeLockOnFailure().

 * FiniteStateMachine.DfProcessQueue.run() check an ARCHIVE lock on
   the dataset before adding a datafile to the list of files to
   archive.

 * FiniteStateMachine.DsProcessQueue.run() check an ARCHIVE lock on
   the dataset before starting a DsArchiver on it.

 * FiniteStateMachine.getServiceStatus() compiles a json string with
   information on currently set locks.

Unsorted Observations
---------------------

 * The delete() web service call throws a BadRequestException if any
   of the concerned dataset has a DELETE lock set.  I'd consider this
   wrong: there is nothing bad in the request, only the resource it
   targets to happens to be currently not available.


Locking in Storage Plugin Option
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

An option on top of the new locking mechanism has been discussed in
the Copenhagen meeting: add a new optional plugin call lock().  This
would allow to use file system locking to also extent the protection
also to external file access.

One issue then will be exception handling: a storage plugin might
call FileChannel.lock() and return the FileLock obtained from this
call.  Both FileChannel.lock() and FileLock.close() throw
IOException.  So we will have to decide how to deal with these
exceptions.
