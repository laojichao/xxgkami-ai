/**
 * 卡密核销接口 /cards/verify 多语言调用示例
 * 请将 BASE_URL 换为您的 API 根路径（含 /api，勿以 / 结尾），如 https://a.com/api 或 http://localhost:8080/api
 */

export const API_USE_CARD_INTRO = `说明：
· 接口路径：{BASE_URL}/cards/verify（仅 POST JSON，与文档一致）
· 必填参数：cardKey、machineCode；可选：apiKeyId（触发回调通知）
· 接口为公开接口，限流 30 次/分钟/IP；machineCode 缺失将被拒绝（防多设备共享）`

export const API_USE_CARD_EXAMPLES = [
  {
    id: 'curl',
    label: 'cURL (POST JSON)',
    code: `# POST JSON
curl -X POST "${'BASE_URL'}/cards/verify" \\
  -H "Content-Type: application/json" \\
  -d '{"cardKey":"YOUR_CARD_KEY","machineCode":"YOUR_MACHINE_CODE"}'`
  },
  {
    id: 'curl-post-form',
    label: 'cURL (POST JSON 紧凑)',
    code: `curl -X POST "${'BASE_URL'}/cards/verify" -H "Content-Type: application/json" -d "{\\"cardKey\\":\\"YOUR_CARD_KEY\\",\\"machineCode\\":\\"YOUR_MACHINE_CODE\\"}"`
  },
  {
    id: 'wget',
    label: 'Wget (POST JSON)',
    code: `wget -qO- --header="Content-Type: application/json" --post-data='{"cardKey":"YOUR_CARD_KEY","machineCode":"YOUR_MACHINE_CODE"}' "${'BASE_URL'}/cards/verify"`
  },
  {
    id: 'httpie',
    label: 'HTTPie',
    code: `# POST JSON
http POST "${'BASE_URL'}/cards/verify" cardKey=YOUR_CARD_KEY machineCode=YOUR_MACHINE_CODE`
  },
  {
    id: 'powershell',
    label: 'PowerShell',
    code: `$base = "BASE_URL"
$body = @{
  cardKey = "YOUR_CARD_KEY"
  machineCode = "YOUR_MACHINE_CODE"
} | ConvertTo-Json
Invoke-RestMethod -Uri "$base/cards/verify" -Method Post -Body $body -ContentType "application/json"`
  },
  {
    id: 'node-fetch',
    label: 'Node.js (fetch)',
    code: `const BASE = process.env.API_BASE || 'BASE_URL';

async function useCard() {
  const res = await fetch(BASE + '/cards/verify', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      cardKey: 'YOUR_CARD_KEY',
      machineCode: 'YOUR_MACHINE_CODE'
    })
  });
  console.log(await res.json());
}

useCard().catch(console.error);`
  },
  {
    id: 'node-axios',
    label: 'Node.js (axios)',
    code: `const axios = require('axios');
const BASE = process.env.API_BASE || 'BASE_URL';

axios.post(BASE + '/cards/verify', {
  cardKey: 'YOUR_CARD_KEY',
  machineCode: 'YOUR_MACHINE_CODE'
}).then(r => console.log(r.data)).catch(console.error);`
  },
  {
    id: 'browser-js',
    label: '浏览器 JavaScript',
    code: `const BASE = 'BASE_URL';

async function redeem() {
  const r = await fetch(BASE + '/cards/verify', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ cardKey: 'YOUR_CARD_KEY', machineCode: 'YOUR_MACHINE_CODE' })
  });
  return r.json();
}

redeem().then(console.log).catch(console.error);`
  },
  {
    id: 'html',
    label: 'HTML + fetch',
    code: `<!DOCTYPE html>
<html><body>
<button id="go">核销</button>
<pre id="out"></pre>
<script>
const BASE = 'BASE_URL';
document.getElementById('go').onclick = async () => {
  const r = await fetch(BASE + '/cards/verify', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      cardKey: document.querySelector('#card').value,
      machineCode: document.querySelector('#machine').value
    })
  });
  document.getElementById('out').textContent = JSON.stringify(await r.json(), null, 2);
};
</script>
<input id="card" placeholder="卡密" />
<input id="machine" placeholder="机器码" />
</body></html>`
  },
  {
    id: 'vue3',
    label: 'Vue 3',
    code: `<script setup>
import { ref } from 'vue'
const BASE = 'BASE_URL'
const result = ref(null)
const loading = ref(false)
const cardKey = ref('')
const machineCode = ref('')

async function redeem() {
  loading.value = true
  try {
    const r = await fetch(BASE + '/cards/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cardKey: cardKey.value, machineCode: machineCode.value })
    })
    result.value = await r.json()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <input v-model="cardKey" placeholder="卡密" />
    <input v-model="machineCode" placeholder="机器码" />
    <button :disabled="loading" @click="redeem">核销</button>
    <pre>{{ result }}</pre>
  </div>
</template>`
  },
  {
    id: 'python-requests',
    label: 'Python (requests)',
    code: `import requests

BASE = "BASE_URL"

r = requests.post(
    f"{BASE}/cards/verify",
    json={
        "cardKey": "YOUR_CARD_KEY",
        "machineCode": "YOUR_MACHINE_CODE",
    },
)
print(r.json())`
  },
  {
    id: 'python-urllib',
    label: 'Python (urllib)',
    code: `import json
import urllib.parse
import urllib.request

BASE = "BASE_URL"

def post_redeem():
    data = json.dumps({
        "cardKey": "YOUR_CARD_KEY",
        "machineCode": "YOUR_MACHINE_CODE",
    }).encode()
    req = urllib.request.Request(
        f"{BASE}/cards/verify",
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode())

print(post_redeem())`
  },
  {
    id: 'php',
    label: 'PHP',
    code: `<?php
$base = 'BASE_URL';
$ch = curl_init($base . '/cards/verify');
curl_setopt_array($ch, [
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => ['Content-Type: application/json'],
    CURLOPT_POSTFIELDS => json_encode([
        'cardKey' => 'YOUR_CARD_KEY',
        'machineCode' => 'YOUR_MACHINE_CODE',
    ]),
    CURLOPT_RETURNTRANSFER => true,
]);
echo curl_exec($ch);`
  },
  {
    id: 'go',
    label: 'Go',
    code: `package main

import (
    "fmt"
    "io"
    "net/http"
    "strings"
)

const base = "BASE_URL"

func main() {
    payload := "{\\"cardKey\\":\\"YOUR_CARD_KEY\\",\\"machineCode\\":\\"YOUR_MACHINE_CODE\\"}"
    resp, err := http.Post(base+"/cards/verify", "application/json", strings.NewReader(payload))
    if err != nil {
        panic(err)
    }
    defer resp.Body.Close()
    body, _ := io.ReadAll(resp.Body)
    fmt.Println(string(body))
}`
  },
  {
    id: 'java',
    label: 'Java (HttpClient)',
    code: `import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UseCard {
    static final String BASE = "BASE_URL";

    public static void main(String[] args) throws Exception {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        String json = "{\\"cardKey\\":\\"YOUR_CARD_KEY\\",\\"machineCode\\":\\"YOUR_MACHINE_CODE\\"}";
        var postReq = HttpRequest.newBuilder()
            .uri(URI.create(BASE + "/cards/verify"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        System.out.println(client.send(postReq, HttpResponse.BodyHandlers.ofString()).body());
    }
}`
  },
  {
    id: 'csharp',
    label: 'C# (.NET)',
    code: `using System.Net.Http;
using System.Text;
using System.Text.Json;

const string Base = "BASE_URL";

using var http = new HttpClient();
var body = JsonSerializer.Serialize(new {
    cardKey = "YOUR_CARD_KEY",
    machineCode = "YOUR_MACHINE_CODE"
});
var postResp = await http.PostAsync($"{Base}/cards/verify",
    new StringContent(body, Encoding.UTF8, "application/json"));
Console.WriteLine(await postResp.Content.ReadAsStringAsync());`
  },
  {
    id: 'ruby',
    label: 'Ruby',
    code: `require 'net/http'
require 'json'
require 'uri'

BASE = 'BASE_URL'

uri = URI("#{BASE}/cards/verify")
req = Net::HTTP::Post.new(uri)
req['Content-Type'] = 'application/json'
req.body = {
  cardKey: 'YOUR_CARD_KEY',
  machineCode: 'YOUR_MACHINE_CODE'
}.to_json
puts Net::HTTP.start(uri.hostname, uri.port, use_ssl: uri.scheme == 'https') { |h| h.request(req).body }`
  },
  {
    id: 'kotlin',
    label: 'Kotlin',
    code: `import java.net.HttpURLConnection
import java.net.URL

fun main() {
    val base = "BASE_URL"
    val body = "{\\"cardKey\\":\\"YOUR_CARD_KEY\\",\\"machineCode\\":\\"YOUR_MACHINE_CODE\\"}"
    val conn = URL("$base/cards/verify").openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/json")
    conn.outputStream.use { it.write(body.toByteArray()) }
    println(conn.inputStream.bufferedReader().readText())
}`
  },
  {
    id: 'dart',
    label: 'Dart (Flutter)',
    code: `import 'dart:convert';
import 'package:http/http.dart' as http;

const base = 'BASE_URL';

Future<void> main() async {
  final r = await http.post(
    Uri.parse('$base/cards/verify'),
    headers: {'Content-Type': 'application/json'},
    body: jsonEncode({
      'cardKey': 'YOUR_CARD_KEY',
      'machineCode': 'YOUR_MACHINE_CODE',
    }),
  );
  print(r.body);
}`
  },
  {
    id: 'rust',
    label: 'Rust (reqwest)',
    code: `#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let base = "BASE_URL";
    let client = reqwest::Client::new();
    let r = client
        .post(format!("{}/cards/verify", base))
        .json(&serde_json::json!({
            "cardKey": "YOUR_CARD_KEY",
            "machineCode": "YOUR_MACHINE_CODE",
        }))
        .send()
        .await?;
    println!("{}", r.text().await?);
    Ok(())
}`
  },
  {
    id: 'swift',
    label: 'Swift',
    code: `import Foundation

let base = "BASE_URL"
var req = URLRequest(url: URL(string: base + "/cards/verify")!)
req.httpMethod = "POST"
req.setValue("application/json", forHTTPHeaderField: "Content-Type")
req.httpBody = try JSONSerialization.data(withJSONObject: [
    "cardKey": "YOUR_CARD_KEY",
    "machineCode": "YOUR_MACHINE_CODE",
])
let (data, _) = try await URLSession.shared.data(for: req)
print(String(data: data, encoding: .utf8)!)`
  }
]
