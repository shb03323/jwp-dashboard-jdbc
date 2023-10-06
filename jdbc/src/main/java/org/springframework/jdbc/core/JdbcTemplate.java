package org.springframework.jdbc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcTemplate {

    private static final Logger log = LoggerFactory.getLogger(JdbcTemplate.class);

    private static final int INITIAL_PARAMETER_INDEX = 1;
    private static final char PLACEHOLDER = '?';

    private final DataSource dataSource;

    public JdbcTemplate(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int update(final Connection connection, final String sql, final Object... args) {
        try (final PreparedStatement ps = createPreparedStatement(connection, sql, args)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new DataAccessException(e);
        }
    }

    public <T> List<T> query(final String sql, final ResultSetMapper<T> resultSetMapper) {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = connection.prepareStatement(sql);
             final ResultSet resultSet = ps.executeQuery()
        ) {
            log.debug("query: {}", sql);
            final List<T> results = new ArrayList<>();
            while(resultSet.next()) {
                results.add(resultSetMapper.apply(resultSet));
            }
            return results;
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new DataAccessException(e);
        }
    }

    public <T> T queryForObject(final String sql, final ResultSetMapper<T> resultSetMapper, final Object... args) {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement ps = createPreparedStatement(connection, sql, args);
             final ResultSet resultSet = ps.executeQuery()
        ) {
            return mapToObject(resultSet, resultSetMapper);
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw new DataAccessException(e);
        }
    }

    private PreparedStatement createPreparedStatement(final Connection connection, final String sql, final Object[] args) throws SQLException {
        log.debug("query: {}", sql);
        final PreparedStatement ps = connection.prepareStatement(sql);
        validateQueryArgs(sql, args);
        setArgs(ps, args);
        return ps;
    }

    private void validateQueryArgs(final String sql, final Object[] args) {
        final long placeholderCount = sql.chars()
                .filter(ch -> ch == PLACEHOLDER)
                .count();
        if (placeholderCount != args.length) {
            throw new InvalidDataAccessApiUsageException();
        }
    }

    private void setArgs(final PreparedStatement ps, final Object[] args) throws SQLException {
        int parameterIndex = INITIAL_PARAMETER_INDEX;
        for (final Object arg : args) {
            ps.setObject(parameterIndex++, arg);
        }
    }

    private <T> T mapToObject(final ResultSet resultSet, final ResultSetMapper<T> resultSetMapper) throws SQLException {
        if (resultSet.next()) {
            final T object = resultSetMapper.apply(resultSet);
            if (resultSet.next()) {
                throw new IncorrectResultSizeDataAccessException();
            }
            return object;
        }
        throw new EmptyResultDataAccessException();
    }
}
