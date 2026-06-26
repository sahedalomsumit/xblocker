package com.sahed.xblocker.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.sahed.xblocker.data.db.AppDatabase
import com.sahed.xblocker.data.db.BlocklistDao
import com.sahed.xblocker.data.db.TimerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideBlocklistDao(db: AppDatabase): BlocklistDao = db.blocklistDao()

    @Provides
    fun provideTimerDao(db: AppDatabase): TimerDao = db.timerDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
