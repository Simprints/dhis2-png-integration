package org.dhis2.usescases.splash


import org.dhis2.data.dagger.PerActivity

import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = arrayOf(SplashModule::class))
interface SplashComponent {
    fun inject(splashActivity: SplashActivity)
}
