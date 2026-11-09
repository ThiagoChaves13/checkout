package br.edu.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ServicoCheckoutTest {
    private static final String CLIENTE = "cliente-1";
    private static final String EMAIL = "cliente@example.com";
    private static final String SKU = "SKU-1";

    @Mock
    private EstoqueGateway estoqueGateway;

    @Mock
    private PagamentoGateway pagamentoGateway;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private Notificador notificador;

    private ServicoCheckout servico;

    @BeforeEach
    void criarServicoReal() {
        servico = new ServicoCheckout(
                estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    @Test
    void deveRejeitarSolicitacaoNulaSemConsultarColaboradores() {
        assertThrows(IllegalArgumentException.class, () -> servico.finalizar(null));
        verifyNoInteractions(
                estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    @ParameterizedTest
    @MethodSource("solicitacoesInvalidas")
    void deveRejeitarDadosInvalidosSemConsultarColaboradores(SolicitacaoCheckout solicitacao) {
        assertThrows(IllegalArgumentException.class, () -> servico.finalizar(solicitacao));
        verifyNoInteractions(estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    static Stream<SolicitacaoCheckout> solicitacoesInvalidas() {
        BigDecimal preco = new BigDecimal("19.90");
        Stream<SolicitacaoCheckout> textosInvalidos = Stream.of(null, "", "  ").flatMap(texto ->
                Stream.of(
                        new SolicitacaoCheckout(texto, EMAIL, SKU, 1, preco),
                        new SolicitacaoCheckout(CLIENTE, texto, SKU, 1, preco),
                        new SolicitacaoCheckout(CLIENTE, EMAIL, texto, 1, preco)));
        Stream<SolicitacaoCheckout> numerosInvalidos = Stream.of(
                new SolicitacaoCheckout(CLIENTE, EMAIL, SKU, 0, preco),
                new SolicitacaoCheckout(CLIENTE, EMAIL, SKU, -1, preco),
                new SolicitacaoCheckout(CLIENTE, EMAIL, SKU, 1, null),
                new SolicitacaoCheckout(CLIENTE, EMAIL, SKU, 1, BigDecimal.ZERO),
                new SolicitacaoCheckout(CLIENTE, EMAIL, SKU, 1, new BigDecimal("-0.01")));
        return Stream.concat(textosInvalidos, numerosInvalidos);
    }

    @Test
    void deveSalvarPedidoEConfirmarCompraAprovadaNaOrdem() {
        SolicitacaoCheckout solicitacao = solicitacao(3, "19.90");
        BigDecimal total = new BigDecimal("59.70");
        when(estoqueGateway.reservar(SKU, 3)).thenReturn(true);
        when(pagamentoGateway.cobrar(CLIENTE, total)).thenReturn(new Pagamento(true, "tx-1"));
        when(pedidoRepository.salvar(any(PedidoConfirmado.class))).thenReturn("pedido-1");

        ResultadoCheckout resultado = servico.finalizar(solicitacao);

        assertEquals(new ResultadoCheckout("pedido-1", total, true), resultado);
        ArgumentCaptor<PedidoConfirmado> captor = ArgumentCaptor.forClass(PedidoConfirmado.class);
        verify(estoqueGateway, times(1)).reservar(SKU, 3);
        verify(pagamentoGateway, times(1)).cobrar(CLIENTE, total);
        verify(pedidoRepository, times(1)).salvar(any(PedidoConfirmado.class));

        InOrder ordem = inOrder(estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
        ordem.verify(estoqueGateway, times(1)).reservar(SKU, 3);
        ordem.verify(pagamentoGateway, times(1)).cobrar(CLIENTE, total);
        ordem.verify(pedidoRepository, times(1)).salvar(captor.capture());
        ordem.verify(notificador, times(1)).enviarConfirmacao(EMAIL, "pedido-1", total);
        assertEquals(new PedidoConfirmado(CLIENTE, EMAIL, SKU, 3, total, "tx-1"),
                captor.getValue());
        verify(estoqueGateway, never()).liberar(anyString(), anyInt());
        verifyNoMoreInteractions(estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    private SolicitacaoCheckout solicitacao(int quantidade, String preco) {
        return new SolicitacaoCheckout(CLIENTE, EMAIL, SKU, quantidade, new BigDecimal(preco));
    }

    @ParameterizedTest
    @CsvSource({"1, 19.90, 19.90", "3, 0.005, 0.015"})
    void deveCalcularTotalExato(int quantidade, String preco, String totalEsperado) {
        BigDecimal total = new BigDecimal(totalEsperado);
        when(estoqueGateway.reservar(SKU, quantidade)).thenReturn(true);
        when(pagamentoGateway.cobrar(CLIENTE, total)).thenReturn(new Pagamento(true, "tx-1"));
        when(pedidoRepository.salvar(any(PedidoConfirmado.class))).thenReturn("pedido-1");

        ResultadoCheckout resultado = servico.finalizar(solicitacao(quantidade, preco));

        assertEquals(total, resultado.total());
        verify(pagamentoGateway, times(1)).cobrar(CLIENTE, total);
    }

    @Test
    void devePararQuandoNaoHaEstoque() {
        when(estoqueGateway.reservar(SKU, 3)).thenReturn(false);

        assertThrows(EstoqueInsuficienteException.class,
                () -> servico.finalizar(solicitacao(3, "19.90")));

        verify(estoqueGateway, times(1)).reservar(SKU, 3);
        verify(estoqueGateway, never()).liberar(anyString(), anyInt());
        verifyNoInteractions(pagamentoGateway, pedidoRepository, notificador);
    }

    @Test
    void deveLiberarReservaDepoisDePagamentoRecusado() {
        BigDecimal total = new BigDecimal("59.70");
        when(estoqueGateway.reservar(SKU, 3)).thenReturn(true);
        when(pagamentoGateway.cobrar(CLIENTE, total)).thenReturn(new Pagamento(false, null));

        assertThrows(PagamentoRecusadoException.class,
                () -> servico.finalizar(solicitacao(3, "19.90")));

        verify(estoqueGateway, times(1)).reservar(SKU, 3);
        verify(pagamentoGateway, times(1)).cobrar(CLIENTE, total);
        verify(estoqueGateway, times(1)).liberar(SKU, 3);

        InOrder ordem = inOrder(estoqueGateway, pagamentoGateway);
        ordem.verify(estoqueGateway, times(1)).reservar(SKU, 3);
        ordem.verify(pagamentoGateway, times(1)).cobrar(CLIENTE, total);
        ordem.verify(estoqueGateway, times(1)).liberar(SKU, 3);
        verifyNoMoreInteractions(estoqueGateway, pagamentoGateway);
        verifyNoInteractions(pedidoRepository, notificador);
    }

    @Test
    void deveLiberarReservaEPropagarMesmaFalhaDePagamento() {
        BigDecimal total = new BigDecimal("59.70");
        FalhaPagamentoException falha = new FalhaPagamentoException("Falha ao cobrar");
        when(estoqueGateway.reservar(SKU, 3)).thenReturn(true);
        when(pagamentoGateway.cobrar(CLIENTE, total)).thenThrow(falha);

        FalhaPagamentoException recebida = assertThrows(FalhaPagamentoException.class,
                () -> servico.finalizar(solicitacao(3, "19.90")));

        assertSame(falha, recebida);
        verify(estoqueGateway, times(1)).reservar(SKU, 3);
        verify(pagamentoGateway, times(1)).cobrar(CLIENTE, total);
        verify(estoqueGateway, times(1)).liberar(SKU, 3);

        InOrder ordem = inOrder(estoqueGateway, pagamentoGateway);
        ordem.verify(estoqueGateway, times(1)).reservar(SKU, 3);
        ordem.verify(pagamentoGateway, times(1)).cobrar(CLIENTE, total);
        ordem.verify(estoqueGateway, times(1)).liberar(SKU, 3);
        verifyNoMoreInteractions(estoqueGateway, pagamentoGateway);
        verifyNoInteractions(pedidoRepository, notificador);
    }

    @Test
    void deveManterPedidoConfirmadoSeNotificacaoFalhar() {
        BigDecimal total = new BigDecimal("59.70");
        when(estoqueGateway.reservar(SKU, 3)).thenReturn(true);
        when(pagamentoGateway.cobrar(CLIENTE, total)).thenReturn(new Pagamento(true, "tx-1"));
        when(pedidoRepository.salvar(any(PedidoConfirmado.class))).thenReturn("pedido-1");
        doThrow(new FalhaNotificacaoException("Falha ao enviar"))
                .when(notificador).enviarConfirmacao(EMAIL, "pedido-1", total);

        ResultadoCheckout resultado = servico.finalizar(solicitacao(3, "19.90"));

        assertEquals(new ResultadoCheckout("pedido-1", total, false), resultado);
        verify(estoqueGateway, times(1)).reservar(SKU, 3);
        verify(pagamentoGateway, times(1)).cobrar(CLIENTE, total);
        verify(pedidoRepository, times(1)).salvar(any(PedidoConfirmado.class));
        verify(notificador, times(1)).enviarConfirmacao(EMAIL, "pedido-1", total);
        verify(estoqueGateway, never()).liberar(anyString(), anyInt());
        verifyNoMoreInteractions(estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }
}
