package ar.edu.utn.frc.bda.depositoclick.catalogo.services;

import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.ArticuloResponse;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.AtributoDTO;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.CategoriaResponse;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.ReglasStockResponse;
import ar.edu.utn.frc.bda.depositoclick.catalogo.dtos.RubroDTO;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Articulo;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.AtributoCategoria;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Categoria;
import ar.edu.utn.frc.bda.depositoclick.catalogo.models.Rubro;

/** Conversiones entidad -> DTO (asi los controllers nunca exponen entidades JPA). */
public final class CatalogoMapper {

    private CatalogoMapper() {
    }

    public static RubroDTO toDTO(Rubro r) {
        return new RubroDTO(r.getId(), r.getNombre(), r.getDescripcion());
    }

    public static AtributoDTO toDTO(AtributoCategoria a) {
        return new AtributoDTO(a.getId(), a.getNombre(), a.getTipo(), a.isObligatorio(),
                a.getListaOpciones().isEmpty() ? null : a.getListaOpciones());
    }

    public static CategoriaResponse toResponse(Categoria c) {
        return CategoriaResponse.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .rubroId(c.getRubro().getId())
                .rubroNombre(c.getRubro().getNombre())
                .controlaLote(c.isControlaLote())
                .controlaVencimiento(c.isControlaVencimiento())
                .atributos(c.getAtributos().stream().map(CatalogoMapper::toDTO).toList())
                .build();
    }

    public static ArticuloResponse toResponse(Articulo a) {
        return ArticuloResponse.builder()
                .id(a.getId())
                .sku(a.getSku())
                .codigoBarras(a.getCodigoBarras())
                .nombre(a.getNombre())
                .descripcion(a.getDescripcion())
                .categoriaId(a.getCategoria().getId())
                .categoriaNombre(a.getCategoria().getNombre())
                .rubroNombre(a.getCategoria().getRubro().getNombre())
                .unidadMedida(a.getUnidadMedida())
                .stockMinimo(a.getStockMinimo())
                .controlaLote(a.getCategoria().isControlaLote())
                .controlaVencimiento(a.getCategoria().isControlaVencimiento())
                .activo(a.isActivo())
                .atributos(a.getAtributosComoMapa())
                .fechaAlta(a.getFechaAlta())
                .fechaModificacion(a.getFechaModificacion())
                .build();
    }

    public static ReglasStockResponse toReglas(Articulo a) {
        return ReglasStockResponse.builder()
                .articuloId(a.getId())
                .sku(a.getSku())
                .nombre(a.getNombre())
                .unidadMedida(a.getUnidadMedida().name())
                .permiteDecimales(a.getUnidadMedida().isPermiteDecimales())
                .controlaLote(a.getCategoria().isControlaLote())
                .controlaVencimiento(a.getCategoria().isControlaVencimiento())
                .stockMinimo(a.getStockMinimo())
                .activo(a.isActivo())
                .build();
    }
}
