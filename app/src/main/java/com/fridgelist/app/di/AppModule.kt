package com.fridgelist.app.di

import android.content.Context
import androidx.room.Room
import com.fridgelist.app.data.db.AppDatabase
import com.fridgelist.app.data.db.TileDao
import com.fridgelist.app.provider.microsoft.MicrosoftTodoApi
import com.fridgelist.app.provider.todoist.TodoistApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "fridgelist.db").build()

    @Provides
    fun provideTileDao(db: AppDatabase): TileDao = db.tileDao()

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideTodoistApi(okHttpClient: OkHttpClient, moshi: Moshi): TodoistApi =
        Retrofit.Builder()
            .baseUrl("https://api.todoist.com/rest/v2/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TodoistApi::class.java)

    @Provides
    @Singleton
    fun provideMicrosoftTodoApi(okHttpClient: OkHttpClient, moshi: Moshi): MicrosoftTodoApi =
        Retrofit.Builder()
            .baseUrl("https://graph.microsoft.com/v1.0/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MicrosoftTodoApi::class.java)
}
