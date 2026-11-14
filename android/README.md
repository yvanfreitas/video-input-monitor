# Video Input Monitor para Android (com Android Auto)

App Android nativo que mostra o vídeo e o áudio de uma **placa de captura USB (UVC)**,
por exemplo um videogame ligado por HDMI, na tela do celular e na tela do carro
pelo Android Auto.

O app é instalado direto, sem passar pela Play Store.

## O que ele faz

- Captura de placas HDMI-USB e webcams UVC via USB-OTG (biblioteca libuvc, funciona sem
  suporte do fabricante do celular).
- Escolha do dispositivo e da resolução. O padrão é 1080p ou 720p em MJPEG, e a escolha fica salva.
- Repassa o áudio da placa (USB Audio) para a saída de mídia. Com o Android Auto, o som sai
  nos alto-falantes do carro.
- Tela cheia, controles que somem após 5 s e Picture-in-Picture (automático ao sair do app).
- **Android Auto**: o vídeo aparece na central do carro **só com o carro parado**. Em
  movimento, a tela do carro mostra um aviso e o vídeo continua no celular para os passageiros.
  A detecção usa a velocidade do carro ou, na falta dela, o GPS do celular. Sem nenhum dado, o
  vídeo fica bloqueado.

## Requisitos

- Android 8.0 ou mais novo, com USB-OTG.
- Placa de captura UVC (a maioria das placas HDMI-USB baratas e das Elgato/AVerMedia mais novas).
- Para usar no carro: **Android Auto sem fio**. Com o Android Auto por cabo, a porta USB do
  celular fica ocupada pelo carro e não sobra porta para a placa.

## Instalação

1. Baixe o APK mais recente em **Releases** (`android-v1.0.N`).
2. Abra o arquivo no celular e permita a instalação de fontes desconhecidas.
3. Abra o app e conceda as permissões:
   - **Microfone**: necessário para capturar o áudio da placa.
   - **Localização**: usada como reserva para detectar se o carro está parado.
   - **Notificações**.
4. Conecte a placa e toque em **Permitir** no pedido de acesso USB. Marque "usar por padrão"
   para não perguntar de novo.

## Habilitar no Android Auto

Apps instalados fora da loja só aparecem no carro com as opções de desenvolvedor:

1. No celular, abra **Configurações › Dispositivos conectados › Android Auto** (ou o app Android Auto).
2. Role até **Versão** e toque nela 10 vezes. Confirme para ativar o modo desenvolvedor.
3. No menu ⋮ › **Configurações do desenvolvedor**, ative **Fontes desconhecidas**.
4. Reconecte ao carro. O "Video Input Monitor" aparece na lista de apps de navegação.

## Atualizações

Cada push em `android/` gera um novo release com o APK. Como todos são assinados com a mesma
chave (`app/sideload.jks`), basta instalar por cima.

Para usar uma chave própria, adicione os secrets `ANDROID_KEYSTORE_PATH`,
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` e `ANDROID_KEY_PASSWORD` e passe-os ao passo de
build do workflow. Trocar de chave exige desinstalar a versão anterior uma vez.

## Compilar localmente

```sh
cd android
./gradlew assembleRelease
# APK em app/build/outputs/apk/release/app-release.apk
```

São necessários o JDK 17 e o Android SDK (API 35).

## Limitações conhecidas

- O Android Auto recomprime a imagem enviada ao carro: há algum atraso (cerca de 100–200 ms) e
  a resolução fica limitada à tela da central.
- Para o áudio continuar com o app em segundo plano, o app precisa ter sido aberto pelo
  menos uma vez na sessão (inicia o serviço em primeiro plano).
