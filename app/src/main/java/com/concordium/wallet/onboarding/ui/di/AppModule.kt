package com.concordium.wallet.onboarding.ui.di

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.concordium.wallet.AppConfig
import com.concordium.wallet.core.config.EnvironmentConfiguration
import com.concordium.wallet.onboarding.data.datasource.OnboardingService
import com.concordium.wallet.onboarding.data.datasource.SharedPreferencesDataSource
import com.concordium.wallet.onboarding.data.datasource.SharedPreferencesDataSourceImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class AppModule {

    val configModule = module {
        single { EnvironmentConfiguration }
    }

    val sharedPreferencesModule = module {
        single {
            MasterKey.Builder(androidContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        }

        single {
            EncryptedSharedPreferences.create(
                androidContext(),
                get<EnvironmentConfiguration>().SHARED_PREFERENCES_FILE_NAME,
                get(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        single<SharedPreferencesDataSource> {
            SharedPreferencesDataSourceImpl(prefs = get())
        }
    }

    val remoteModule = module {
        single {
            val timeout = 10L

            OkHttpClient.Builder()
                .apply {
                    connectTimeout(timeout, TimeUnit.SECONDS).readTimeout(timeout, TimeUnit.SECONDS)
                }
                .addInterceptor(
                    HttpLoggingInterceptor()
                        .apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        })
                .build()
        }

        single<Retrofit> {
            Retrofit.Builder()
                .addConverterFactory(
                    MoshiConverterFactory.create(
                        Moshi.Builder()
                            .addLast(KotlinJsonAdapterFactory())
                            .build()
                    )
                )
                .baseUrl(AppConfig.proxyBaseUrl)
                .client(get())
                .build()
        }
    }

    val apiModule = module {
        single { get<Retrofit>().create(OnboardingService::class.java) }
    }
}
