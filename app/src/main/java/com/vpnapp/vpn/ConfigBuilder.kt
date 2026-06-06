package com.vpnapp.vpn

import com.vpnapp.Constants
import com.vpnapp.api.model.SingBoxOutbound
import com.vpnapp.api.model.VpnNode
import org.json.JSONArray
import org.json.JSONObject

object ConfigBuilder {

    fun buildV2RayConfig(node: VpnNode): String {
        val config = JSONObject()

        // Log
        config.put("log", JSONObject().apply {
            put("loglevel", "warning")
        })

        // DNS
        config.put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                put("1.1.1.1")
                put("8.8.8.8")
            })
        })

        // Inbounds
        config.put("inbounds", JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "socks")
                put("port", Constants.SOCKS_PORT)
                put("protocol", "socks")
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http"); put("tls")
                    })
                })
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                    put("ip", "127.0.0.1")
                })
            })
            put(JSONObject().apply {
                put("tag", "http")
                put("port", Constants.HTTP_PROXY_PORT)
                put("protocol", "http")
                put("settings", JSONObject().apply {
                    put("allowTransparent", false)
                })
            })
        })

        // Outbound
        val outboundObj = buildOutbound(node) ?: defaultOutbound()
        config.put("outbounds", JSONArray().apply {
            put(outboundObj)
            put(JSONObject().apply {
                put("tag", "direct")
                put("protocol", "freedom")
                put("settings", JSONObject())
            })
            put(JSONObject().apply {
                put("tag", "block")
                put("protocol", "blackhole")
                put("settings", JSONObject().apply {
                    put("response", JSONObject().apply { put("type", "http") })
                })
            })
        })

        // Routing
        config.put("routing", JSONObject().apply {
            put("domainStrategy", "IPIfNonMatch")
            put("rules", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "field")
                    put("ip", JSONArray().apply {
                        put("geoip:private")
                        put("geoip:cn")
                    })
                    put("outboundTag", "direct")
                })
                put(JSONObject().apply {
                    put("type", "field")
                    put("domain", JSONArray().apply {
                        put("geosite:cn")
                    })
                    put("outboundTag", "direct")
                })
            })
        })

        return config.toString(2)
    }

    private fun buildOutbound(node: VpnNode): JSONObject? {
        return when (node.type.lowercase()) {
            "vmess" -> buildVmess(node)
            "vless" -> buildVless(node)
            "trojan" -> buildTrojan(node)
            "shadowsocks" -> buildShadowsocks(node)
            else -> null
        }
    }

    private fun buildVmess(node: VpnNode): JSONObject {
        val raw = node.rawConfig
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vmess")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", node.server)
                        put("port", node.port)
                        put("users", JSONArray().apply {
                            put(JSONObject().apply {
                                put("id", raw.uuid ?: "")
                                put("alterId", raw.alterId ?: 0)
                                put("security", raw.security ?: "auto")
                                put("level", 8)
                            })
                        })
                    })
                })
            })
            put("streamSettings", buildStreamSettings(raw))
        }
    }

    private fun buildVless(node: VpnNode): JSONObject {
        val raw = node.rawConfig
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "vless")
            put("settings", JSONObject().apply {
                put("vnext", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", node.server)
                        put("port", node.port)
                        put("users", JSONArray().apply {
                            put(JSONObject().apply {
                                put("id", raw.uuid ?: "")
                                put("encryption", "none")
                                put("level", 8)
                                if (raw.tls?.reality?.enabled == true) {
                                    put("flow", "xtls-rprx-vision")
                                }
                            })
                        })
                    })
                })
            })
            put("streamSettings", buildStreamSettings(raw))
        }
    }

    private fun buildTrojan(node: VpnNode): JSONObject {
        val raw = node.rawConfig
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "trojan")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", node.server)
                        put("port", node.port)
                        put("password", raw.password ?: "")
                        put("level", 8)
                    })
                })
            })
            put("streamSettings", buildStreamSettings(raw))
        }
    }

    private fun buildShadowsocks(node: VpnNode): JSONObject {
        val raw = node.rawConfig
        return JSONObject().apply {
            put("tag", "proxy")
            put("protocol", "shadowsocks")
            put("settings", JSONObject().apply {
                put("servers", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", node.server)
                        put("port", node.port)
                        put("method", raw.method ?: "aes-256-gcm")
                        put("password", raw.password ?: "")
                        put("level", 8)
                    })
                })
            })
        }
    }

    private fun buildStreamSettings(raw: SingBoxOutbound): JSONObject {
        val transport = raw.transport
        val tls = raw.tls
        val network = transport?.type ?: raw.network ?: "tcp"

        return JSONObject().apply {
            put("network", network)

            when (network.lowercase()) {
                "ws" -> put("wsSettings", JSONObject().apply {
                    put("path", transport?.path ?: "/")
                    val headers = transport?.headers
                    if (!headers.isNullOrEmpty()) {
                        put("headers", JSONObject().apply {
                            headers.forEach { (k, v) -> put(k, v) }
                        })
                    }
                })
                "grpc" -> put("grpcSettings", JSONObject().apply {
                    put("serviceName", transport?.service_name ?: "")
                    put("multiMode", false)
                })
                "http" -> put("httpSettings", JSONObject().apply {
                    put("path", transport?.path ?: "/")
                })
            }

            if (tls?.enabled == true) {
                val tlsSettings = JSONObject().apply {
                    put("allowInsecure", tls.insecure)
                    if (!tls.server_name.isNullOrEmpty()) put("serverName", tls.server_name)
                    if (!tls.alpn.isNullOrEmpty()) put("alpn", JSONArray(tls.alpn))
                    if (tls.utls?.enabled == true && !tls.utls.fingerprint.isNullOrEmpty()) {
                        put("fingerprint", tls.utls.fingerprint)
                    }
                    if (tls.reality?.enabled == true) {
                        put("publicKey", tls.reality.public_key ?: "")
                        put("shortId", tls.reality.short_id ?: "")
                    }
                }
                if (tls.reality?.enabled == true) {
                    put("security", "reality")
                    put("realitySettings", tlsSettings)
                } else {
                    put("security", "tls")
                    put("tlsSettings", tlsSettings)
                }
            }
        }
    }

    private fun defaultOutbound() = JSONObject().apply {
        put("tag", "proxy")
        put("protocol", "freedom")
        put("settings", JSONObject())
    }
}
