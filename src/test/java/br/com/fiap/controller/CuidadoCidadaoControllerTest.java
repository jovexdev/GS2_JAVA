package br.com.fiap.controller;

import br.com.fiap.bo.OrientacaoCidadaoBO;
import br.com.fiap.bo.UnidadesSaudeBO;
import br.com.fiap.entities.Perfil;
import br.com.fiap.entities.Usuario;
import br.com.fiap.excecoes.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CuidadoCidadaoControllerTest {
    @Test
    void exigeSessaoEPerfilCidadaoAntesDeConsultarServicos() {
        var unidades=mock(UnidadesSaudeBO.class);var orientacao=mock(OrientacaoCidadaoBO.class);
        var controller=new CuidadoCidadaoController(unidades,orientacao);
        var request=new MockHttpServletRequest();
        assertThatThrownBy(()->controller.unidades(3550308,request)).isInstanceOf(ApiException.class);
        for(var perfil:new Perfil[]{Perfil.EMPRESA,Perfil.GESTOR,Perfil.AGENTE}) {
            Sessao.iniciar(request,new Usuario(1,perfil,"Teste",null,"teste@example.test",null,null,null));
            assertThatThrownBy(()->controller.unidades(3550308,request)).isInstanceOf(ApiException.class);
            assertThatThrownBy(()->controller.orientar(null,request)).isInstanceOf(ApiException.class);
        }
        verifyNoInteractions(unidades,orientacao);
        Sessao.iniciar(request,new Usuario(1,Perfil.CIDADAO,"Teste",null,"teste@example.test",null,null,null));
        controller.unidades(3550308,request);verify(unidades).buscar(3550308);
        assertThat(controller.orientar(null,request).getHeaders().getCacheControl()).isEqualTo("no-store");
    }
}
