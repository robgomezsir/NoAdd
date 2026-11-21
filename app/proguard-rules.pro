# NoAdd não usa bibliotecas de terceiros, então não há nada específico a
# preservar: o AGP gera as regras que mantêm as Activities, o activity-alias e
# os pontos de entrada declarados no AndroidManifest.xml.
#
# O arquivo proguard-android-optimize.txt já liga o R8 em full mode, remove
# atributos de debug e faz otimizações de código.
#
# Se algum dia for necessário ofuscar nomes de pacote (economiza poucos KB):
# -repackageclasses 'a'
