package com.vortex.launcher.di

import android.content.Context
import androidx.room.Room
import com.vortex.launcher.data.db.ProfileDao
import com.vortex.launcher.data.db.VortexDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun db(@ApplicationContext c: Context): VortexDatabase =
        Room.databaseBuilder(c, VortexDatabase::class.java, "vortex.db").build()

    @Provides fun profileDao(db: VortexDatabase): ProfileDao = db.profileDao()
}
