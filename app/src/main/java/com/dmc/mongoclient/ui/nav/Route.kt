package com.dmc.mongoclient.ui.nav

object Route {
    const val CONNECTIONS = "connections"
    const val CONNECTION_EDIT = "connection/edit"
    const val CONNECTION_EDIT_ARG = "id"
    fun connectionEdit(id: Long? = null) =
        if (id == null) "$CONNECTION_EDIT/new" else "$CONNECTION_EDIT/$id"

    const val BROWSE = "browse"

    const val SETTINGS = "settings"
}
