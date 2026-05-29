package br.com.zeusapp.stella.connector;

import br.com.zeusapp.stella.connector.exception.BancoConnectorException;
import br.com.zeusapp.stella.dto.BancoConfig;
import br.com.zeusapp.stella.dto.BoletoConsultaDTO;
import br.com.zeusapp.stella.dto.BoletoDTO;
import br.com.zeusapp.stella.dto.BoletoRegistradoDTO;

/**
 * Contrato de integração com a API de cobrança de um banco.
 * Cada implementação gerencia autenticação (OAuth2 ou mTLS) de forma transparente.
 */
public interface BancoConnector {

    /**
     * Registra o boleto na API do banco.
     * O retorno contém linhaDigitavel, codigoBarras e, quando disponível, emvPix/txid.
     */
    BoletoRegistradoDTO registrar(BoletoDTO boleto) throws BancoConnectorException;

    /**
     * Consulta a situação atual de um boleto pelo nosso número.
     */
    BoletoConsultaDTO consultar(String nossoNumero) throws BancoConnectorException;

    /**
     * Solicita a baixa/cancelamento do boleto no banco.
     */
    void cancelar(String nossoNumero) throws BancoConnectorException;

    /** Código FEBRABAN do banco suportado por este connector ("001", "237", "341", "748"). */
    String codigoBanco();

    /** Configuração de credenciais em uso por esta instância do connector. */
    BancoConfig getConfig();
}
