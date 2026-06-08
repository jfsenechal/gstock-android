package be.marche.gstock.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import be.marche.gstock.data.local.entity.CheckoutEntity
import be.marche.gstock.data.local.entity.ToolEntity
import be.marche.gstock.data.local.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY lastName, firstName")
    fun observeAll(): Flow<List<WorkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(workers: List<WorkerEntity>)

    @Query("DELETE FROM workers")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(workers: List<WorkerEntity>) {
        clear()
        upsertAll(workers)
    }
}

@Dao
interface ToolDao {
    @Query("SELECT * FROM tools ORDER BY name")
    fun observeAll(): Flow<List<ToolEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tools: List<ToolEntity>)

    @Query("DELETE FROM tools")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(tools: List<ToolEntity>) {
        clear()
        upsertAll(tools)
    }
}

@Dao
interface CheckoutDao {
    @Query("SELECT * FROM checkouts ORDER BY checkedOutAt DESC")
    fun observeAll(): Flow<List<CheckoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(checkouts: List<CheckoutEntity>)

    @Query("DELETE FROM checkouts")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(checkouts: List<CheckoutEntity>) {
        clear()
        upsertAll(checkouts)
    }
}
