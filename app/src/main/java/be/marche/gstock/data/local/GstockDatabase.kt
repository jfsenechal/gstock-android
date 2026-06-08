package be.marche.gstock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import be.marche.gstock.data.local.dao.AuthDao
import be.marche.gstock.data.local.dao.CheckoutDao
import be.marche.gstock.data.local.dao.ToolDao
import be.marche.gstock.data.local.dao.WorkerDao
import be.marche.gstock.data.local.entity.AuthEntity
import be.marche.gstock.data.local.entity.CheckoutEntity
import be.marche.gstock.data.local.entity.ToolEntity
import be.marche.gstock.data.local.entity.WorkerEntity

@Database(
    entities = [WorkerEntity::class, ToolEntity::class, CheckoutEntity::class, AuthEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class GstockDatabase : RoomDatabase() {
    abstract fun workerDao(): WorkerDao
    abstract fun toolDao(): ToolDao
    abstract fun checkoutDao(): CheckoutDao
    abstract fun authDao(): AuthDao
}
