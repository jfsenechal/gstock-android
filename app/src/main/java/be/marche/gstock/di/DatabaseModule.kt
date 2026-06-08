package be.marche.gstock.di

import android.content.Context
import androidx.room.Room
import be.marche.gstock.data.local.GstockDatabase
import be.marche.gstock.data.local.dao.AuthDao
import be.marche.gstock.data.local.dao.CheckoutDao
import be.marche.gstock.data.local.dao.ToolDao
import be.marche.gstock.data.local.dao.WorkerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GstockDatabase =
        Room.databaseBuilder(context, GstockDatabase::class.java, "gstock.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideWorkerDao(db: GstockDatabase): WorkerDao = db.workerDao()

    @Provides
    fun provideToolDao(db: GstockDatabase): ToolDao = db.toolDao()

    @Provides
    fun provideCheckoutDao(db: GstockDatabase): CheckoutDao = db.checkoutDao()

    @Provides
    fun provideAuthDao(db: GstockDatabase): AuthDao = db.authDao()
}
