package com.campuslink.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.room.Room
import com.campuslink.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    @Provides @Singleton
    fun provideBluetoothAdapter(@ApplicationContext ctx: Context): BluetoothAdapter {
        val manager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter
    }
    
    @Provides @Singleton
    fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "campuslink.db").build()
        
    @Provides fun provideMessageDao(db: AppDatabase) = db.messageDao()
    @Provides fun provideUserDao(db: AppDatabase)    = db.userDao()
    @Provides fun providePerfLogDao(db: AppDatabase) = db.performanceLogDao()
}
