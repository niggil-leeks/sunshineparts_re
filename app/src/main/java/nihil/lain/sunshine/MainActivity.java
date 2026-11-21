package nihil.lain.sunshine;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/* JADX INFO: loaded from: classes2.dex */
public final class MainActivity extends Activity {
    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        int i = (int) (getResources().getDisplayMetrics().density * 24.0f);
        linearLayout.setPadding(i, i * 2, i, i);
        linearLayout.setBackgroundColor(Color.rgb(20, 22, 25));
        text(linearLayout, "Sunshine UI", 30, -13210);
        text(linearLayout, "1.7.0 · Vector / LSPosed", 15, -4144183);
        boolean z = Build.DISPLAY != null && Build.DISPLAY.contains("Sunshine") && Build.VERSION.SDK_INT == 36;
        text(linearLayout, z ? "ROM compatível" : "Módulo inativo nesta ROM", 21, z ? -6494540 : -19300);
        text(linearLayout, z ? "Ative este módulo no Vector, selecione apenas Interface do sistema (com.android.systemui) e reinicie o aparelho." : "Os ajustes exigem Android 16 e Sunshine em ro.build.display.id.", 17, -986896);
        text(linearLayout, "Cortina e animações\nAnimações e frequência originais do sistema por padrão. O voto extra de frequência pode ser testado pelo controle, sem reiniciar.", 17, -2039584);
        text(linearLayout, "Player com música\nA onda da barra de progresso usa menos pontos por quadro. A animação, a capa e os botões continuam funcionando.", 17, -2039584);
        text(linearLayout, "Abertura das barras\nA preparação do fundo desfocado ocorre em paralelo. A barra pode continuar animando enquanto o fundo fica pronto, inclusive na tela de bloqueio.", 17, -2039584);
        text(linearLayout, "Tiles e painel\nReutiliza a instância do painel ao reconectar, evitando criar cópias sobrepostas. Também evita reaplicar o mesmo layout quando o cabeçalho continua com a mesma geometria. Mudanças de tamanho e configuração continuam funcionando. As animações são preservadas.", 17, -2039584);
        text(linearLayout, "Ajustes sem reiniciar\nOs grupos de captura, layout e animação podem ser ligados ou desligados por comandos root. Consulte o controle.sh incluído nos fontes.", 17, -2039584);
        text(linearLayout, "Esta tela confirma a compatibilidade, não a ativação no Vector. Para desfazer, desative o módulo e reinicie.", 15, -4144183);
        text(linearLayout, "Crédito do ajuste stepX: Samsung Music QS Lag Fix (UN1CA), DanGLVK. github.com/Xposed-Modules-Repo/com.samsung.music.qs.lagfix", 13, -6709850);
        text(linearLayout, Build.DISPLAY, 13, -6709850);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.addView(linearLayout);
        setContentView(scrollView);
    }

    void text(LinearLayout linearLayout, String str, int i, int i2) {
        TextView textView = new TextView(this);
        textView.setText(str);
        textView.setTextSize(i);
        textView.setTextColor(i2);
        textView.setPadding(0, 0, 0, (int) (getResources().getDisplayMetrics().density * 18.0f));
        linearLayout.addView(textView);
    }
}
